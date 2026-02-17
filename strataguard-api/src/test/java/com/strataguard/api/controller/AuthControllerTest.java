package com.strataguard.api.controller;

import com.strataguard.core.dto.auth.AuthResponse;
import com.strataguard.core.dto.auth.LoginRequest;
import com.strataguard.core.dto.auth.RefreshTokenRequest;
import com.strataguard.core.dto.auth.RegisterRequest;
import com.strataguard.core.dto.auth.RegisterResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private static final String USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333").toString();
    private static final String TENANT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444").toString();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("POST /register - Register User")
    class Register {

        @Test
        @DisplayName("should return 201 CREATED with registration data and success message")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("admin@strataguard.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("SecurePass123!")
                    .role("ESTATE_ADMIN")
                    .build();

            RegisterResponse expectedResponse = RegisterResponse.builder()
                    .userId(USER_ID)
                    .email("admin@strataguard.com")
                    .role("ESTATE_ADMIN")
                    .tenantId(TENANT_ID)
                    .build();

            when(authService.register(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<RegisterResponse>> result = authController.register(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("User registered successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getEmail()).isEqualTo("admin@strataguard.com");
            assertThat(result.getBody().getData().getRole()).isEqualTo("ESTATE_ADMIN");

            verify(authService).register(request);
        }
    }

    @Nested
    @DisplayName("POST /login - Login")
    class Login {

        @Test
        @DisplayName("should return 200 OK with auth tokens and success message")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .email("admin@strataguard.com")
                    .password("SecurePass123!")
                    .build();

            AuthResponse expectedResponse = AuthResponse.builder()
                    .accessToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.mock-access-token")
                    .refreshToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.mock-refresh-token")
                    .expiresIn(300L)
                    .tokenType("Bearer")
                    .build();

            when(authService.login(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<AuthResponse>> result = authController.login(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Login successful");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getAccessToken()).isNotBlank();
            assertThat(result.getBody().getData().getRefreshToken()).isNotBlank();
            assertThat(result.getBody().getData().getTokenType()).isEqualTo("Bearer");

            verify(authService).login(request);
        }
    }

    @Nested
    @DisplayName("POST /refresh - Refresh Token")
    class RefreshToken {

        @Test
        @DisplayName("should return 200 OK with new auth tokens and success message")
        void shouldRefreshTokenSuccessfully() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.mock-refresh-token")
                    .build();

            AuthResponse expectedResponse = AuthResponse.builder()
                    .accessToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.new-access-token")
                    .refreshToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.new-refresh-token")
                    .expiresIn(300L)
                    .tokenType("Bearer")
                    .build();

            when(authService.refreshToken(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<AuthResponse>> result = authController.refreshToken(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Token refreshed successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            assertThat(result.getBody().getData().getAccessToken()).contains("new-access-token");
            assertThat(result.getBody().getData().getRefreshToken()).contains("new-refresh-token");

            verify(authService).refreshToken(request);
        }
    }
}
