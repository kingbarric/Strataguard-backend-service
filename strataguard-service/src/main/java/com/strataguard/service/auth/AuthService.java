package com.strataguard.service.auth;

import com.strataguard.core.dto.auth.*;
import com.strataguard.core.enums.UserRole;
import com.strataguard.service.keycloak.KeycloakUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KeycloakUserService keycloakUserService;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.admin.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    public RegisterResponse register(RegisterRequest request) {
        validateRole(request.getRole());

        String tenantId = UUID.randomUUID().toString();

        String userId = keycloakUserService.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                request.getRole(),
                tenantId
        );

        log.info("Registered user: {} with role: {} and tenant: {}", request.getEmail(), request.getRole(), tenantId);

        return RegisterResponse.builder()
                .userId(userId)
                .email(request.getEmail())
                .role(request.getRole())
                .tenantId(tenantId)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        return requestToken(
                "grant_type=password"
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret)
                        + "&username=" + encode(request.getEmail())
                        + "&password=" + encode(request.getPassword())
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return requestToken(
                "grant_type=refresh_token"
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret)
                        + "&refresh_token=" + encode(request.getRefreshToken())
        );
    }

    private AuthResponse requestToken(String formBody) {
        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                return AuthResponse.builder()
                        .accessToken(json.get("access_token").asText())
                        .refreshToken(json.get("refresh_token").asText())
                        .expiresIn(json.get("expires_in").asLong())
                        .tokenType(json.get("token_type").asText())
                        .build();
            } else {
                log.error("Keycloak token request failed: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("Authentication failed: invalid credentials");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to connect to Keycloak: {}", e.getMessage());
            throw new RuntimeException("Authentication service unavailable", e);
        }
    }

    private void validateRole(String role) {
        try {
            UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role
                    + ". Valid roles: SUPER_ADMIN, ESTATE_ADMIN, FACILITY_MANAGER, RESIDENT, SECURITY_GUARD");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
