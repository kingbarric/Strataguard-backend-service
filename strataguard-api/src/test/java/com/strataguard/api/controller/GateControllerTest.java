package com.strataguard.api.controller;

import com.strataguard.core.dto.accesslog.GateAccessLogResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.gate.*;
import com.strataguard.core.enums.GateEventType;
import com.strataguard.core.enums.GateSessionStatus;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import com.strataguard.service.gate.GateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GateController")
class GateControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private GateService gateService;

    @InjectMocks
    private GateController gateController;

    // ---------------------------------------------------------------
    // processEntry
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /entry - processEntry")
    class ProcessEntry {

        @Test
        @DisplayName("should return 201 CREATED with entry response and success message")
        void shouldReturn201WithEntryResponse() {
            // Arrange
            GateEntryRequest request = new GateEntryRequest();
            request.setQrStickerCode("QR-ABC-123");
            request.setNote("Visitor parking lot B");

            GateEntryResponse serviceResponse = GateEntryResponse.builder()
                    .sessionId(SESSION_ID)
                    .status(GateSessionStatus.OPEN)
                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                    .vehicleId(VEHICLE_ID)
                    .plateNumber("KAA 123A")
                    .make("Toyota")
                    .model("Corolla")
                    .color("White")
                    .vehicleType(VehicleType.CAR)
                    .vehicleStatus(VehicleStatus.ACTIVE)
                    .residentId(RESIDENT_ID)
                    .residentFirstName("John")
                    .residentLastName("Doe")
                    .residentPhone("+254700000000")
                    .build();

            when(gateService.processEntry(request)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<GateEntryResponse>> result = gateController.processEntry(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle entry processed successfully");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getSessionId()).isEqualTo(SESSION_ID);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(GateSessionStatus.OPEN);

            verify(gateService).processEntry(request);
        }
    }

    // ---------------------------------------------------------------
    // processExit
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /exit - processExit")
    class ProcessExit {

        @Test
        @DisplayName("should return 200 OK with exit response and success message")
        void shouldReturn200WithExitResponse() {
            // Arrange
            GateExitRequest request = new GateExitRequest();
            request.setQrStickerCode("QR-ABC-123");
            request.setExitPassToken("valid-token");
            request.setNote("Exiting normally");

            GateExitResponse serviceResponse = GateExitResponse.builder()
                    .sessionId(SESSION_ID)
                    .status(GateSessionStatus.CLOSED)
                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                    .exitTime(Instant.parse("2026-02-17T17:00:00Z"))
                    .vehicleId(VEHICLE_ID)
                    .plateNumber("KAA 123A")
                    .residentId(RESIDENT_ID)
                    .residentFirstName("John")
                    .residentLastName("Doe")
                    .build();

            when(gateService.processExit(request)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<GateExitResponse>> result = gateController.processExit(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle exit processed successfully");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(GateSessionStatus.CLOSED);

            verify(gateService).processExit(request);
        }
    }

    // ---------------------------------------------------------------
    // processRemoteApprovalExit
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /exit/remote/{sessionId} - processRemoteApprovalExit")
    class ProcessRemoteApprovalExit {

        @Test
        @DisplayName("should return 200 OK with exit response and remote approval message")
        void shouldReturn200WithRemoteApprovalExitResponse() {
            // Arrange
            GateExitResponse serviceResponse = GateExitResponse.builder()
                    .sessionId(SESSION_ID)
                    .status(GateSessionStatus.CLOSED)
                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                    .exitTime(Instant.parse("2026-02-17T17:00:00Z"))
                    .vehicleId(VEHICLE_ID)
                    .plateNumber("KAA 123A")
                    .residentId(RESIDENT_ID)
                    .residentFirstName("John")
                    .residentLastName("Doe")
                    .build();

            when(gateService.processRemoteApprovalExit(SESSION_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<GateExitResponse>> result = gateController.processRemoteApprovalExit(SESSION_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle exit via remote approval processed successfully");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);

            verify(gateService).processRemoteApprovalExit(SESSION_ID);
        }
    }

    // ---------------------------------------------------------------
    // getSession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /sessions/{id} - getSession")
    class GetSession {

        @Test
        @DisplayName("should return 200 OK with session response")
        void shouldReturn200WithSessionResponse() {
            // Arrange
            GateSessionResponse serviceResponse = GateSessionResponse.builder()
                    .id(SESSION_ID)
                    .vehicleId(VEHICLE_ID)
                    .residentId(RESIDENT_ID)
                    .plateNumber("KAA 123A")
                    .status(GateSessionStatus.OPEN)
                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                    .entryGuardId("guard-user-id")
                    .entryNote("Entry note")
                    .createdAt(Instant.parse("2026-02-17T08:00:00Z"))
                    .updatedAt(Instant.parse("2026-02-17T08:00:00Z"))
                    .build();

            when(gateService.getSession(SESSION_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<GateSessionResponse>> result = gateController.getSession(SESSION_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(SESSION_ID);

            verify(gateService).getSession(SESSION_ID);
        }
    }

    // ---------------------------------------------------------------
    // getAllSessions
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /sessions - getAllSessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return 200 OK with paged session response")
        void shouldReturn200WithPagedSessionResponse() {
            // Arrange
            PagedResponse<GateSessionResponse> serviceResponse = PagedResponse.<GateSessionResponse>builder()
                    .content(List.of(
                            GateSessionResponse.builder()
                                    .id(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .plateNumber("KAA 123A")
                                    .status(GateSessionStatus.OPEN)
                                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getAllSessions(DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> result =
                    gateController.getAllSessions(DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(gateService).getAllSessions(DEFAULT_PAGEABLE);
        }
    }

    // ---------------------------------------------------------------
    // getOpenSessions
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /sessions/open - getOpenSessions")
    class GetOpenSessions {

        @Test
        @DisplayName("should return 200 OK with paged open session response")
        void shouldReturn200WithPagedOpenSessionResponse() {
            // Arrange
            PagedResponse<GateSessionResponse> serviceResponse = PagedResponse.<GateSessionResponse>builder()
                    .content(List.of(
                            GateSessionResponse.builder()
                                    .id(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .plateNumber("KAA 123A")
                                    .status(GateSessionStatus.OPEN)
                                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getOpenSessions(DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> result =
                    gateController.getOpenSessions(DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getContent().get(0).getStatus()).isEqualTo(GateSessionStatus.OPEN);

            verify(gateService).getOpenSessions(DEFAULT_PAGEABLE);
        }
    }

    // ---------------------------------------------------------------
    // getSessionsByVehicle
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /sessions/vehicle/{vehicleId} - getSessionsByVehicle")
    class GetSessionsByVehicle {

        @Test
        @DisplayName("should return 200 OK with paged sessions for a vehicle")
        void shouldReturn200WithPagedSessionsForVehicle() {
            // Arrange
            PagedResponse<GateSessionResponse> serviceResponse = PagedResponse.<GateSessionResponse>builder()
                    .content(List.of(
                            GateSessionResponse.builder()
                                    .id(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .plateNumber("KAA 123A")
                                    .status(GateSessionStatus.CLOSED)
                                    .entryTime(Instant.parse("2026-02-17T08:00:00Z"))
                                    .exitTime(Instant.parse("2026-02-17T17:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getSessionsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateSessionResponse>>> result =
                    gateController.getSessionsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getContent().get(0).getVehicleId()).isEqualTo(VEHICLE_ID);

            verify(gateService).getSessionsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE);
        }
    }

    // ---------------------------------------------------------------
    // getAllAccessLogs
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /logs - getAllAccessLogs")
    class GetAllAccessLogs {

        @Test
        @DisplayName("should return 200 OK with paged access log response")
        void shouldReturn200WithPagedAccessLogResponse() {
            // Arrange
            PagedResponse<GateAccessLogResponse> serviceResponse = PagedResponse.<GateAccessLogResponse>builder()
                    .content(List.of(
                            GateAccessLogResponse.builder()
                                    .id(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                                    .sessionId(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .eventType(GateEventType.ENTRY_SCAN)
                                    .guardId("guard-user-id")
                                    .details("Vehicle entered gate")
                                    .success(true)
                                    .createdAt(Instant.parse("2026-02-17T08:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getAllAccessLogs(DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> result =
                    gateController.getAllAccessLogs(DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(gateService).getAllAccessLogs(DEFAULT_PAGEABLE);
        }
    }

    // ---------------------------------------------------------------
    // getAccessLogsBySession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /logs/session/{sessionId} - getAccessLogsBySession")
    class GetAccessLogsBySession {

        @Test
        @DisplayName("should return 200 OK with paged access logs for a session")
        void shouldReturn200WithPagedAccessLogsForSession() {
            // Arrange
            PagedResponse<GateAccessLogResponse> serviceResponse = PagedResponse.<GateAccessLogResponse>builder()
                    .content(List.of(
                            GateAccessLogResponse.builder()
                                    .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
                                    .sessionId(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .eventType(GateEventType.EXIT_SCAN)
                                    .guardId("guard-user-id")
                                    .details("Vehicle exited gate")
                                    .success(true)
                                    .createdAt(Instant.parse("2026-02-17T17:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getAccessLogsBySession(SESSION_ID, DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> result =
                    gateController.getAccessLogsBySession(SESSION_ID, DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getContent().get(0).getSessionId()).isEqualTo(SESSION_ID);

            verify(gateService).getAccessLogsBySession(SESSION_ID, DEFAULT_PAGEABLE);
        }
    }

    // ---------------------------------------------------------------
    // getAccessLogsByVehicle
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /logs/vehicle/{vehicleId} - getAccessLogsByVehicle")
    class GetAccessLogsByVehicle {

        @Test
        @DisplayName("should return 200 OK with paged access logs for a vehicle")
        void shouldReturn200WithPagedAccessLogsForVehicle() {
            // Arrange
            PagedResponse<GateAccessLogResponse> serviceResponse = PagedResponse.<GateAccessLogResponse>builder()
                    .content(List.of(
                            GateAccessLogResponse.builder()
                                    .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
                                    .sessionId(SESSION_ID)
                                    .vehicleId(VEHICLE_ID)
                                    .residentId(RESIDENT_ID)
                                    .eventType(GateEventType.ENTRY_SCAN)
                                    .guardId("guard-user-id")
                                    .details("Vehicle entered gate")
                                    .success(true)
                                    .createdAt(Instant.parse("2026-02-17T08:00:00Z"))
                                    .build()
                    ))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(gateService.getAccessLogsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<PagedResponse<GateAccessLogResponse>>> result =
                    gateController.getAccessLogsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getContent().get(0).getVehicleId()).isEqualTo(VEHICLE_ID);

            verify(gateService).getAccessLogsByVehicle(VEHICLE_ID, DEFAULT_PAGEABLE);
        }
    }
}
