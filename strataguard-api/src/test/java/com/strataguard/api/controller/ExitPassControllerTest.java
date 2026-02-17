package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.exitpass.ExitPassResponse;
import com.strataguard.service.gate.ExitPassService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitPassController")
class ExitPassControllerTest {

    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ExitPassService exitPassService;

    @InjectMocks
    private ExitPassController exitPassController;

    // ---------------------------------------------------------------
    // generateExitPass
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /vehicle/{vehicleId} - generateExitPass")
    class GenerateExitPass {

        @Test
        @DisplayName("should return 200 OK with exit pass response and success message")
        void shouldReturn200WithExitPassResponse() {
            // Arrange
            ExitPassResponse serviceResponse = ExitPassResponse.builder()
                    .vehicleId(VEHICLE_ID)
                    .token("base64-encoded-payload.hmac-signature")
                    .expiresAt(Instant.parse("2026-02-17T08:05:00Z"))
                    .build();

            when(exitPassService.generateExitPass(VEHICLE_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<ExitPassResponse>> result =
                    exitPassController.generateExitPass(VEHICLE_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Exit pass generated successfully");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(result.getBody().getData().getToken()).isEqualTo("base64-encoded-payload.hmac-signature");
            assertThat(result.getBody().getData().getExpiresAt()).isEqualTo(Instant.parse("2026-02-17T08:05:00Z"));

            verify(exitPassService).generateExitPass(VEHICLE_ID);
        }
    }
}
