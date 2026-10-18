package com.vidhika.auth_service.controller;

import com.vidhika.auth_service.dto.AuthResponse;
import com.vidhika.auth_service.dto.LoginRequest;
import com.vidhika.auth_service.dto.OAuthLoginRequest;
import com.vidhika.auth_service.dto.RegisterRequest;
import com.vidhika.auth_service.service.AuthService;
import com.vidhika.auth_service.service.GithubOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GithubOAuthService githubOAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Browser Entry Point: Visit this URL in the browser to start GitHub OAuth login.
     * Redirects browser to GitHub's authorization consent screen.
     */
    @GetMapping("/github/login")
    public ResponseEntity<Void> redirectToGithub() {
        String authUrl = githubOAuthService.getAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Browser Callback: GitHub redirects the browser here with ?code=...
     * Exchanges code for tokens and returns AuthResponse JSON.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<AuthResponse> handleGithubCallback(@RequestParam("code") String code) {
        AuthResponse response = authService.loginWithGithub(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Programmatic / Postman Entry Point: Accepts JSON with {"code": "..."}
     */
    @PostMapping("/github")
    public ResponseEntity<AuthResponse> loginWithGithubJson(@Valid @RequestBody OAuthLoginRequest request) {
        AuthResponse response = authService.loginWithGithub(request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        authService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    @PostMapping("/upgrade-to-organizer")
    public ResponseEntity<AuthResponse> upgradeToOrganizer(@AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse response = authService.upgradeToOrganizer(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}