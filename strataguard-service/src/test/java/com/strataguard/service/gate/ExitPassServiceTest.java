package com.strataguard.service.gate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.exitpass.ExitPassResponse;
import com.strataguard.core.entity.Vehicle;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import com.strataguard.core.exception.GateAccessDeniedException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitPassService")
class ExitPassServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String CURRENT_USER_ID = "user-keycloak-id-001";
    private static final String TEST_SECRET = "test-exit-secret";
    private static final int EXPIRY_SECONDS = 300;

    @Mock
    private VehicleRepository vehicleRepository;

    private ExitPassService exitPassService;

    @BeforeEach
    void setUp() {
        exitPassService = new ExitPassService(vehicleRepository, TEST_SECRET, EXPIRY_SECONDS);
        TenantContext.setTenantId(TENANT_ID);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(CURRENT_USER_ID);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // Helper builders
    // ---------------------------------------------------------------

    private Vehicle buildVehicleWithResident() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setTenantId(TENANT_ID);
        vehicle.setResidentId(RESIDENT_ID);
        vehicle.setPlateNumber("KAA123A");
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        return vehicle;
    }

    private Vehicle buildVehicleWithoutResident() {
        Vehicle vehicle = buildVehicleWithResident();
        vehicle.setResidentId(null);
        return vehicle;
    }

    // ---------------------------------------------------------------
    // generateExitPass
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("generateExitPass")
    class GenerateExitPass {

        @Test
        @DisplayName("should return ExitPassResponse with vehicleId, non-null token, and future expiresAt")
        void shouldReturnValidExitPassResponse() {
            // Arrange
            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitPassResponse response = exitPassService.generateExitPass(VEHICLE_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getToken()).isNotNull().isNotEmpty();
            assertThat(response.getToken()).contains(".");
            assertThat(response.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found")
        void shouldThrowWhenVehicleNotFound() {
            // Arrange
            UUID unknownVehicleId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(vehicleRepository.findByIdAndTenantId(unknownVehicleId, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exitPassService.generateExitPass(unknownVehicleId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when vehicle has null residentId")
        void shouldThrowWhenVehicleHasNoResident() {
            // Arrange
            Vehicle vehicle = buildVehicleWithoutResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act & Assert
            assertThatThrownBy(() -> exitPassService.generateExitPass(VEHICLE_ID))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("Vehicle has no associated resident");
        }
    }

    // ---------------------------------------------------------------
    // validateExitPass
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("validateExitPass")
    class ValidateExitPass {

        @Test
        @DisplayName("should return true for a valid token with matching vehicleId and tenantId")
        void shouldReturnTrueForValidToken() {
            // Arrange
            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            ExitPassResponse response = exitPassService.generateExitPass(VEHICLE_ID);

            // Act
            boolean result = exitPassService.validateExitPass(response.getToken(), VEHICLE_ID, TENANT_ID);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when vehicleId does not match")
        void shouldReturnFalseForWrongVehicleId() {
            // Arrange
            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            ExitPassResponse response = exitPassService.generateExitPass(VEHICLE_ID);
            UUID differentVehicleId = UUID.fromString("00000000-0000-0000-0000-000000000088");

            // Act
            boolean result = exitPassService.validateExitPass(response.getToken(), differentVehicleId, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when tenantId does not match")
        void shouldReturnFalseForWrongTenantId() {
            // Arrange
            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            ExitPassResponse response = exitPassService.generateExitPass(VEHICLE_ID);
            UUID differentTenantId = UUID.fromString("00000000-0000-0000-0000-000000000099");

            // Act
            boolean result = exitPassService.validateExitPass(response.getToken(), VEHICLE_ID, differentTenantId);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when token has expired")
        void shouldReturnFalseForExpiredToken() {
            // Arrange — create a service with 0 expiry seconds to produce an immediately-expired token
            ExitPassService zeroExpiryService = new ExitPassService(vehicleRepository, TEST_SECRET, 0);

            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            ExitPassResponse response = zeroExpiryService.generateExitPass(VEHICLE_ID);

            // Small delay to ensure expiry
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            boolean result = zeroExpiryService.validateExitPass(response.getToken(), VEHICLE_ID, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for token with invalid format (no dot)")
        void shouldReturnFalseForInvalidFormat() {
            // Act
            boolean result = exitPassService.validateExitPass("invalid-token-no-dot", VEHICLE_ID, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when signature is tampered")
        void shouldReturnFalseForTamperedSignature() {
            // Arrange
            Vehicle vehicle = buildVehicleWithResident();
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            ExitPassResponse response = exitPassService.generateExitPass(VEHICLE_ID);
            String token = response.getToken();
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + "TAMPERED";

            // Act
            boolean result = exitPassService.validateExitPass(tamperedToken, VEHICLE_ID, TENANT_ID);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
