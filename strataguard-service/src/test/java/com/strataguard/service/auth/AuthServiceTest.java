package com.strataguard.service.auth;

import com.strataguard.core.dto.auth.RegisterRequest;
import com.strataguard.core.dto.auth.RegisterResponse;
import com.strataguard.core.enums.UserRole;
import com.strataguard.service.keycloak.KeycloakUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(authService, "realm", "test-realm");
        ReflectionTestUtils.setField(authService, "clientId", "test-client");
        ReflectionTestUtils.setField(authService, "clientSecret", "test-secret");
    }

    // ========================================================================
    // register
    // ========================================================================

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequest registerRequest;

        @BeforeEach
        void setUp() {
            registerRequest = RegisterRequest.builder()
                    .email("john.doe@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("securePassword123")
                    .role("ESTATE_ADMIN")
                    .build();
        }

        @Test
        @DisplayName("should register user successfully and return RegisterResponse")
        void shouldRegisterUserSuccessfully() {
            when(keycloakUserService.createUser(
                    eq("john.doe@example.com"),
                    eq("John"),
                    eq("Doe"),
                    eq("securePassword123"),
                    eq("ESTATE_ADMIN"),
                    anyString()
            )).thenReturn("generated-user-id");

            RegisterResponse result = authService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("generated-user-id");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getRole()).isEqualTo("ESTATE_ADMIN");
            assertThat(result.getTenantId()).isNotNull().isNotEmpty();

            verify(keycloakUserService).createUser(
                    eq("john.doe@example.com"),
                    eq("John"),
                    eq("Doe"),
                    eq("securePassword123"),
                    eq("ESTATE_ADMIN"),
                    anyString()
            );
        }

        @Test
        @DisplayName("should generate a unique tenantId for each registration")
        void shouldGenerateUniqueTenantId() {
            when(keycloakUserService.createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            )).thenReturn("user-id-1");

            RegisterResponse result1 = authService.register(registerRequest);
            RegisterResponse result2 = authService.register(registerRequest);

            assertThat(result1.getTenantId()).isNotNull();
            assertThat(result2.getTenantId()).isNotNull();
            assertThat(result1.getTenantId()).isNotEqualTo(result2.getTenantId());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when role is invalid")
        void shouldThrowIllegalArgumentExceptionWhenRoleIsInvalid() {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("john.doe@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .password("securePassword123")
                    .role("INVALID_ROLE")
                    .build();

            assertThatThrownBy(() -> authService.register(invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid role")
                    .hasMessageContaining("INVALID_ROLE");

            verify(keycloakUserService, never()).createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should propagate exception when keycloakUserService.createUser fails")
        void shouldPropagateExceptionWhenCreateUserFails() {
            when(keycloakUserService.createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            )).thenThrow(new RuntimeException("Keycloak unavailable"));

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Keycloak unavailable");
        }
    }

    // ========================================================================
    // validateRole
    // ========================================================================

    @Nested
    @DisplayName("validateRole")
    class ValidateRole {

        @ParameterizedTest(name = "should accept valid role: {0}")
        @EnumSource(UserRole.class)
        @DisplayName("should accept all valid UserRole values")
        void shouldAcceptAllValidRoles(UserRole role) {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .password("password123")
                    .role(role.name())
                    .build();

            when(keycloakUserService.createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            )).thenReturn("user-id");

            RegisterResponse result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(role.name());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty role string")
        void shouldThrowIllegalArgumentExceptionForEmptyRole() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .password("password123")
                    .role("")
                    .build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid role");

            verify(keycloakUserService, never()).createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for lowercase role")
        void shouldThrowIllegalArgumentExceptionForLowercaseRole() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("user@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .password("password123")
                    .role("estate_admin")
                    .build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid role")
                    .hasMessageContaining("estate_admin");

            verify(keycloakUserService, never()).createUser(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
        }
    }

    // ========================================================================
    // login and refreshToken
    // ========================================================================

    /*
     * login() and refreshToken() are NOT unit-tested here because they internally
     * create an HttpClient via HttpClient.newHttpClient() — a static factory call
     * that cannot be mocked with Mockito without PowerMock or refactoring.
     *
     * These methods should be covered by integration tests that spin up a real
     * (or WireMock-based) Keycloak token endpoint.
     *
     * To make them unit-testable, the HttpClient should be injected as a
     * constructor dependency rather than created inline.
     */
}
