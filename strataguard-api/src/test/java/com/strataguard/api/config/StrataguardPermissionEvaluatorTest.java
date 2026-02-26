package com.strataguard.api.config;

import com.strataguard.core.config.EstateContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("StrataguardPermissionEvaluator — hasPermission checks")
class StrataguardPermissionEvaluatorTest {

    @InjectMocks
    private StrataguardPermissionEvaluator evaluator;

    @AfterEach
    void cleanup() {
        EstateContext.clear();
    }

    private Authentication buildAuth(String... roles) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("user-1")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        return new JwtAuthenticationToken(jwt, authorities, "user-1");
    }

    @Test
    @DisplayName("null authentication returns false")
    void nullAuth() {
        assertThat(evaluator.hasPermission(null, null, "estate.read")).isFalse();
    }

    @Test
    @DisplayName("null permission returns false")
    void nullPermission() {
        assertThat(evaluator.hasPermission(buildAuth("ESTATE_ADMIN"), null, null)).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN always returns true regardless of EstateContext")
    void superAdminBypass() {
        // EstateContext is empty — normally would deny, but SUPER_ADMIN bypasses
        assertThat(evaluator.hasPermission(buildAuth("SUPER_ADMIN"), null, "estate.delete")).isTrue();
        assertThat(evaluator.hasPermission(buildAuth("SUPER_ADMIN"), null, "anything.at.all")).isTrue();
    }

    @Test
    @DisplayName("non-admin with matching permission in EstateContext returns true")
    void permissionGranted() {
        EstateContext.setPermissions(Set.of("estate.read", "unit.read"));
        assertThat(evaluator.hasPermission(buildAuth("ESTATE_ADMIN"), null, "estate.read")).isTrue();
    }

    @Test
    @DisplayName("non-admin without matching permission returns false")
    void permissionDenied() {
        EstateContext.setPermissions(Set.of("estate.read"));
        assertThat(evaluator.hasPermission(buildAuth("ESTATE_ADMIN"), null, "estate.delete")).isFalse();
    }

    @Test
    @DisplayName("empty EstateContext permissions returns false for non-admin")
    void emptyPermissions() {
        // No EstateContext set — e.g. missing X-ESTATE-ID
        assertThat(evaluator.hasPermission(buildAuth("ESTATE_ADMIN"), null, "estate.read")).isFalse();
    }

    @Test
    @DisplayName("hasPermission(auth, targetId, targetType, permission) delegates correctly")
    void serializedOverload() {
        EstateContext.setPermissions(Set.of("gate.entry"));
        assertThat(evaluator.hasPermission(buildAuth("SECURITY_GUARD"), "id-123", "Gate", "gate.entry")).isTrue();
        assertThat(evaluator.hasPermission(buildAuth("SECURITY_GUARD"), "id-123", "Gate", "gate.exit")).isFalse();
    }
}
