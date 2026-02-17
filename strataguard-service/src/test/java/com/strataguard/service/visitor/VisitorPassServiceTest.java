package com.strataguard.service.visitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisitorPassService")
class VisitorPassServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-hmac";
    private static final UUID VISITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String PASS_CODE = "PASS-ABC-123";
    private static final long VALID_TO_MILLIS = System.currentTimeMillis() + 86_400_000L; // +24h

    private VisitorPassService visitorPassService;

    @BeforeEach
    void setUp() {
        visitorPassService = new VisitorPassService(TEST_SECRET);
    }

    // ---------------------------------------------------------------
    // generateToken
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should produce a token that is not null or empty")
        void shouldProduceNonNullNonEmptyToken() {
            // Act
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Assert
            assertThat(token).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("should produce a token containing a dot separator")
        void shouldContainDotSeparator() {
            // Act
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Assert
            assertThat(token).contains(".");
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(2);
            assertThat(parts[0]).isNotEmpty();
            assertThat(parts[1]).isNotEmpty();
        }

        @Test
        @DisplayName("should produce different tokens on each call due to nonce")
        void shouldProduceDifferentTokensDueToNonce() {
            // Act
            String token1 = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);
            String token2 = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ---------------------------------------------------------------
    // validateToken
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("should return true for a valid token with matching tenantId")
        void shouldReturnTrueForValidToken() {
            // Arrange
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Act
            boolean result = visitorPassService.validateToken(token, TENANT_ID);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when tenantId does not match")
        void shouldReturnFalseForWrongTenant() {
            // Arrange
            UUID differentTenantId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Act
            boolean result = visitorPassService.validateToken(token, differentTenantId);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for token with invalid format (no dot)")
        void shouldReturnFalseForInvalidFormat() {
            // Act
            boolean result = visitorPassService.validateToken("invalid-token-without-dot", TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when signature is tampered")
        void shouldReturnFalseForTamperedSignature() {
            // Arrange
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + "TAMPERED";

            // Act
            boolean result = visitorPassService.validateToken(tamperedToken, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when payload is tampered")
        void shouldReturnFalseForTamperedPayload() {
            // Arrange
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);
            String[] parts = token.split("\\.");
            // Modify the first character of the base64 payload
            String tamperedPayload = "X" + parts[0].substring(1);
            String tamperedToken = tamperedPayload + "." + parts[1];

            // Act
            boolean result = visitorPassService.validateToken(tamperedToken, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // Act
            boolean result = visitorPassService.validateToken("", TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            // Act
            boolean result = visitorPassService.validateToken(null, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // extractPassCode
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("extractPassCode")
    class ExtractPassCode {

        @Test
        @DisplayName("should return the passCode used during token generation")
        void shouldReturnPassCodeFromValidToken() {
            // Arrange
            String token = visitorPassService.generateToken(PASS_CODE, VISITOR_ID, TENANT_ID, VALID_TO_MILLIS);

            // Act
            String extractedPassCode = visitorPassService.extractPassCode(token);

            // Assert
            assertThat(extractedPassCode).isEqualTo(PASS_CODE);
        }

        @Test
        @DisplayName("should return null for an invalid token")
        void shouldReturnNullForInvalidToken() {
            // Act
            String extractedPassCode = visitorPassService.extractPassCode("totally-invalid-token");

            // Assert
            assertThat(extractedPassCode).isNull();
        }
    }
}
