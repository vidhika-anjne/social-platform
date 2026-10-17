package com.vidhika.auth_service.service;

import com.vidhika.auth_service.dto.AuthResponse;
import com.vidhika.auth_service.dto.GithubUserResponse;
import com.vidhika.auth_service.dto.LoginRequest;
import com.vidhika.auth_service.dto.RegisterRequest;
import com.vidhika.auth_service.enums.AuthProvider;
import com.vidhika.auth_service.enums.OutboxStatus;
import com.vidhika.auth_service.enums.Role;
import com.vidhika.auth_service.model.OutboxEvent;
import com.vidhika.auth_service.model.RefreshToken;
import com.vidhika.auth_service.model.User;
import com.vidhika.auth_service.repository.OutboxEventRepository;
import com.vidhika.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GithubOAuthService githubOAuthService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);

        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateId(savedUser.getId())
                .eventType("USER_REGISTERED")
                .payload("""
                        {
                          "eventId": "%s",
                          "eventType": "USER_REGISTERED",
                          "aggregateId": "%s",
                          "userId": "%s",
                          "email": "%s",
                          "name": "%s",
                          "createdAt": "%s"
                        }
                        """.formatted(
                                eventId,
                                savedUser.getId(),
                                savedUser.getId(),
                                savedUser.getEmail(),
                                savedUser.getName(),
                                createdAt
                        ))
                .createdAt(createdAt)
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(event);

        String accessToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse loginWithGithub(String code) {
        // 1. Exchange code for GitHub access token
        String githubAccessToken = githubOAuthService.getAccessToken(code);

        // 2. Fetch GitHub profile
        GithubUserResponse githubProfile = githubOAuthService.getUserProfile(githubAccessToken);

        if (githubProfile.getEmail() == null || githubProfile.getEmail().isBlank()) {
            throw new RuntimeException("Could not retrieve a verified email address from your GitHub account");
        }

        // 3. Find existing user by GitHub provider & ID, or by email, or create new user
        User user = userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, githubProfile.getId())
                .orElseGet(() -> {
                    // Check if a user with this email already exists (e.g. registered locally)
                    return userRepository.findByEmail(githubProfile.getEmail())
                            .map(existingUser -> {
                                // Link GitHub provider to existing user
                                existingUser.setProvider(AuthProvider.GITHUB);
                                existingUser.setProviderId(githubProfile.getId());
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> registerOAuthUser(githubProfile));
                });

        // 4. Issue Pulse JWT and Refresh Token
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User {} successfully authenticated via GitHub OAuth", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    private User registerOAuthUser(GithubUserResponse githubProfile) {
        String displayName = (githubProfile.getName() != null && !githubProfile.getName().isBlank())
                ? githubProfile.getName()
                : githubProfile.getLogin();

        User newUser = User.builder()
                .name(displayName)
                .email(githubProfile.getEmail())
                .password(null) // OAuth users do not have a local password
                .role(Role.USER)
                .provider(AuthProvider.GITHUB)
                .providerId(githubProfile.getId())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Registered new user {} via GitHub OAuth with ID: {}", savedUser.getEmail(), savedUser.getId());

        // Emit USER_REGISTERED event to Kafka via Outbox so user-service sets up their profile
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateId(savedUser.getId())
                .eventType("USER_REGISTERED")
                .payload("""
                        {
                          "eventId": "%s",
                          "eventType": "USER_REGISTERED",
                          "aggregateId": "%s",
                          "userId": "%s",
                          "email": "%s",
                          "name": "%s",
                          "createdAt": "%s"
                        }
                        """.formatted(
                                eventId,
                                savedUser.getId(),
                                savedUser.getId(),
                                savedUser.getEmail(),
                                savedUser.getName(),
                                createdAt
                        ))
                .createdAt(createdAt)
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(event);

        return savedUser;
    }

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenService.deleteByUser(user);
    }

    @Transactional
    public void deleteAccount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenService.deleteByUser(user);
        userRepository.delete(user);
    }
}