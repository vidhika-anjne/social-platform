package com.vidhika.auth_service.service;

import com.vidhika.auth_service.dto.AuthResponse;
import com.vidhika.auth_service.dto.LoginRequest;
import com.vidhika.auth_service.dto.RegisterRequest;
import com.vidhika.auth_service.enums.OutboxStatus;
import com.vidhika.auth_service.enums.Role;
import com.vidhika.auth_service.model.OutboxEvent;
import com.vidhika.auth_service.model.RefreshToken;
import com.vidhika.auth_service.model.User;
import com.vidhika.auth_service.repository.OutboxEventRepository;
import com.vidhika.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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