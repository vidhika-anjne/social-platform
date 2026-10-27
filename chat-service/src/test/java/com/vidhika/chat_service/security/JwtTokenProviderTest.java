package com.vidhika.chat_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secret);
    }

    private String createTestToken(String subject, String email, String role, long expirationMillis) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("email", email, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = createTestToken("user-123", "user@pulse.com", "DEVELOPER", 60000);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user-123", jwtTokenProvider.extractUserId(token));
        assertEquals("user@pulse.com", jwtTokenProvider.extractEmail(token));
        assertEquals("DEVELOPER", jwtTokenProvider.extractRole(token));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        String token = createTestToken("user-123", "user@pulse.com", "DEVELOPER", -1000);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testValidateToken_MalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
