package com.booking.security;

import com.booking.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-that-is-long-enough-for-hs256-algorithm-123456");
        properties.setExpirationMs(3_600_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtService.generateToken(1L, "admin", Role.ADMIN);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(token, "admin")).isTrue();
        assertThat(jwtService.isTokenValid(token, "other")).isFalse();
    }
}
