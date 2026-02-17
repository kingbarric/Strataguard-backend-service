package com.strataguard.service.gate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.accesslog.GateAccessLogResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.gate.*;
import com.strataguard.core.entity.GateAccessLog;
import com.strataguard.core.entity.GateSession;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.Vehicle;
import com.strataguard.core.enums.GateEventType;
import com.strataguard.core.enums.GateSessionStatus;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import com.strataguard.core.exception.BlacklistedException;
import com.strataguard.core.exception.GateAccessDeniedException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.GateAccessLogMapper;
import com.strataguard.core.util.GateSessionMapper;
import com.strataguard.infrastructure.repository.BlacklistRepository;
import com.strataguard.infrastructure.repository.GateAccessLogRepository;
import com.strataguard.infrastructure.repository.GateSessionRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GateService")
class GateServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final String GUARD_ID = "guard-user-id";
    private static final String QR_STICKER_CODE = "QR-ABC-123";
    private static final String PLATE_NUMBER = "KAA 123A";
    private static final String NORMALIZED_PLATE = "KAA123A";
    private static final String EXIT_PASS_TOKEN = "valid-exit-pass-token";

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private GateSessionRepository gateSessionRepository;

    @Mock
    private GateAccessLogRepository gateAccessLogRepository;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private GateSessionMapper gateSessionMapper;

    @Mock
    private GateAccessLogMapper gateAccessLogMapper;

    @Mock
    private ExitPassService exitPassService;

    @InjectMocks
    private GateService gateService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(GUARD_ID);
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

    private Vehicle buildActiveVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setTenantId(TENANT_ID);
        vehicle.setResidentId(RESIDENT_ID);
        vehicle.setPlateNumber(PLATE_NUMBER);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setQrStickerCode(QR_STICKER_CODE);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        return vehicle;
    }

    private Resident buildResident() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        resident.setPhone("+254700000000");
        resident.setEmail("john.doe@example.com");
        return resident;
    }

    private GateSession buildOpenSession() {
        GateSession session = new GateSession();
        session.setId(SESSION_ID);
        session.setTenantId(TENANT_ID);
        session.setVehicleId(VEHICLE_ID);
        session.setResidentId(RESIDENT_ID);
        session.setPlateNumber(PLATE_NUMBER);
        session.setStatus(GateSessionStatus.OPEN);
        session.setEntryTime(Instant.parse("2026-02-17T08:00:00Z"));
        session.setEntryGuardId(GUARD_ID);
        session.setEntryNote("Entry note");
        return session;
    }

    private GateSession buildClosedSession() {
        GateSession session = buildOpenSession();
        session.setStatus(GateSessionStatus.CLOSED);
        session.setExitTime(Instant.parse("2026-02-17T17:00:00Z"));
        session.setExitGuardId(GUARD_ID);
        session.setExitNote("Exit note");
        return session;
    }

    private GateEntryRequest buildEntryRequest() {
        GateEntryRequest request = new GateEntryRequest();
        request.setQrStickerCode(QR_STICKER_CODE);
        request.setNote("Visitor parking lot B");
        return request;
    }

    private GateExitRequest buildExitRequest() {
        GateExitRequest request = new GateExitRequest();
        request.setQrStickerCode(QR_STICKER_CODE);
        request.setExitPassToken(EXIT_PASS_TOKEN);
        request.setNote("Exiting normally");
        return request;
    }

    private GateSessionResponse buildSessionResponse(GateSession session) {
        return GateSessionResponse.builder()
                .id(session.getId())
                .vehicleId(session.getVehicleId())
                .residentId(session.getResidentId())
                .plateNumber(session.getPlateNumber())
                .status(session.getStatus())
                .entryTime(session.getEntryTime())
                .exitTime(session.getExitTime())
                .entryGuardId(session.getEntryGuardId())
                .exitGuardId(session.getExitGuardId())
                .entryNote(session.getEntryNote())
                .exitNote(session.getExitNote())
                .build();
    }

    private GateAccessLogResponse buildAccessLogResponse() {
        return GateAccessLogResponse.builder()
                .id(UUID.randomUUID())
                .sessionId(SESSION_ID)
                .vehicleId(VEHICLE_ID)
                .residentId(RESIDENT_ID)
                .eventType(GateEventType.ENTRY_SCAN)
                .guardId(GUARD_ID)
                .details("Vehicle entered gate")
                .success(true)
                .createdAt(Instant.now())
                .build();
    }

    // ---------------------------------------------------------------
    // processEntry
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("processEntry")
    class ProcessEntry {

        @Test
        @DisplayName("should create session and log entry event on successful entry")
        void shouldCreateSessionAndLogEntryEvent() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            Vehicle vehicle = buildActiveVehicle();
            Resident resident = buildResident();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(blacklistRepository.isPlateBlacklisted(NORMALIZED_PLATE, TENANT_ID))
                    .thenReturn(false);
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(gateSessionRepository.save(any(GateSession.class)))
                    .thenAnswer(invocation -> {
                        GateSession saved = invocation.getArgument(0);
                        saved.setId(SESSION_ID);
                        return saved;
                    });

            // Act
            GateEntryResponse response = gateService.processEntry(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(response.getStatus()).isEqualTo(GateSessionStatus.OPEN);
            assertThat(response.getEntryTime()).isNotNull();
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getPlateNumber()).isEqualTo(PLATE_NUMBER);
            assertThat(response.getMake()).isEqualTo("Toyota");
            assertThat(response.getModel()).isEqualTo("Corolla");
            assertThat(response.getColor()).isEqualTo("White");
            assertThat(response.getVehicleType()).isEqualTo(VehicleType.CAR);
            assertThat(response.getVehicleStatus()).isEqualTo(VehicleStatus.ACTIVE);
            assertThat(response.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(response.getResidentFirstName()).isEqualTo("John");
            assertThat(response.getResidentLastName()).isEqualTo("Doe");
            assertThat(response.getResidentPhone()).isEqualTo("+254700000000");

            // Verify session was saved with correct fields
            ArgumentCaptor<GateSession> sessionCaptor = ArgumentCaptor.forClass(GateSession.class);
            verify(gateSessionRepository).save(sessionCaptor.capture());
            GateSession savedSession = sessionCaptor.getValue();
            assertThat(savedSession.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(savedSession.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(savedSession.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(savedSession.getPlateNumber()).isEqualTo(PLATE_NUMBER);
            assertThat(savedSession.getStatus()).isEqualTo(GateSessionStatus.OPEN);
            assertThat(savedSession.getEntryGuardId()).isEqualTo(GUARD_ID);
            assertThat(savedSession.getEntryNote()).isEqualTo("Visitor parking lot B");

            // Verify access log event was recorded (ENTRY_SCAN)
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(savedLog.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(savedLog.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(savedLog.getEventType()).isEqualTo(GateEventType.ENTRY_SCAN);
            assertThat(savedLog.getGuardId()).isEqualTo(GUARD_ID);
            assertThat(savedLog.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found by QR code")
        void shouldThrowWhenVehicleNotFound() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.processEntry(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("qrStickerCode")
                    .hasMessageContaining(QR_STICKER_CODE);

            verify(gateSessionRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BlacklistedException and log denial when vehicle is blacklisted")
        void shouldThrowWhenVehicleIsBlacklisted() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            Vehicle vehicle = buildActiveVehicle();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(blacklistRepository.isPlateBlacklisted(NORMALIZED_PLATE, TENANT_ID))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> gateService.processEntry(request))
                    .isInstanceOf(BlacklistedException.class)
                    .hasMessageContaining("blacklisted")
                    .hasMessageContaining(NORMALIZED_PLATE);

            // Verify denial event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog denialLog = logCaptor.getValue();
            assertThat(denialLog.getEventType()).isEqualTo(GateEventType.VEHICLE_DENIED_BLACKLIST);
            assertThat(denialLog.isSuccess()).isFalse();
            assertThat(denialLog.getVehicleId()).isEqualTo(VEHICLE_ID);

            // Verify no session was created
            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when vehicle is not active")
        void shouldThrowWhenVehicleNotActive() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            Vehicle vehicle = buildActiveVehicle();
            vehicle.setStatus(VehicleStatus.REMOVED);

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(blacklistRepository.isPlateBlacklisted(NORMALIZED_PLATE, TENANT_ID))
                    .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> gateService.processEntry(request))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("not active")
                    .hasMessageContaining("REMOVED");

            // Verify denial event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog denialLog = logCaptor.getValue();
            assertThat(denialLog.getEventType()).isEqualTo(GateEventType.ENTRY_SCAN);
            assertThat(denialLog.isSuccess()).isFalse();

            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when vehicle has duplicate open session")
        void shouldThrowWhenDuplicateOpenSession() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            Vehicle vehicle = buildActiveVehicle();
            GateSession existingSession = buildOpenSession();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(blacklistRepository.isPlateBlacklisted(NORMALIZED_PLATE, TENANT_ID))
                    .thenReturn(false);
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.of(existingSession));

            // Act & Assert
            assertThatThrownBy(() -> gateService.processEntry(request))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("already has an open gate session");

            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found for vehicle")
        void shouldThrowWhenResidentNotFound() {
            // Arrange
            GateEntryRequest request = buildEntryRequest();
            Vehicle vehicle = buildActiveVehicle();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(blacklistRepository.isPlateBlacklisted(NORMALIZED_PLATE, TENANT_ID))
                    .thenReturn(false);
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.processEntry(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident");

            verify(gateSessionRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // processExit
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("processExit")
    class ProcessExit {

        @Test
        @DisplayName("should close session and log exit events on successful exit")
        void shouldCloseSessionAndLogExitEvents() {
            // Arrange
            GateExitRequest request = buildExitRequest();
            Vehicle vehicle = buildActiveVehicle();
            GateSession openSession = buildOpenSession();
            Resident resident = buildResident();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.of(openSession));
            when(exitPassService.validateExitPass(EXIT_PASS_TOKEN, VEHICLE_ID, TENANT_ID))
                    .thenReturn(true);
            when(gateSessionRepository.save(any(GateSession.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            // Act
            GateExitResponse response = gateService.processExit(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(response.getStatus()).isEqualTo(GateSessionStatus.CLOSED);
            assertThat(response.getEntryTime()).isEqualTo(Instant.parse("2026-02-17T08:00:00Z"));
            assertThat(response.getExitTime()).isNotNull();
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getPlateNumber()).isEqualTo(PLATE_NUMBER);
            assertThat(response.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(response.getResidentFirstName()).isEqualTo("John");
            assertThat(response.getResidentLastName()).isEqualTo("Doe");

            // Verify session was updated to CLOSED
            ArgumentCaptor<GateSession> sessionCaptor = ArgumentCaptor.forClass(GateSession.class);
            verify(gateSessionRepository).save(sessionCaptor.capture());
            GateSession closedSession = sessionCaptor.getValue();
            assertThat(closedSession.getStatus()).isEqualTo(GateSessionStatus.CLOSED);
            assertThat(closedSession.getExitTime()).isNotNull();
            assertThat(closedSession.getExitGuardId()).isEqualTo(GUARD_ID);
            assertThat(closedSession.getExitNote()).isEqualTo("Exiting normally");

            // Verify two access log events: EXIT_PASS_VALIDATED and EXIT_SCAN
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository, times(2)).save(logCaptor.capture());
            List<GateAccessLog> logs = logCaptor.getAllValues();
            assertThat(logs.get(0).getEventType()).isEqualTo(GateEventType.EXIT_PASS_VALIDATED);
            assertThat(logs.get(0).isSuccess()).isTrue();
            assertThat(logs.get(1).getEventType()).isEqualTo(GateEventType.EXIT_SCAN);
            assertThat(logs.get(1).isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found by QR code")
        void shouldThrowWhenVehicleNotFound() {
            // Arrange
            GateExitRequest request = buildExitRequest();
            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.processExit(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle");

            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when no open session found for vehicle")
        void shouldThrowWhenNoOpenSessionFound() {
            // Arrange
            GateExitRequest request = buildExitRequest();
            Vehicle vehicle = buildActiveVehicle();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.processExit(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("GateSession");

            verify(exitPassService, never()).validateExitPass(any(), any(), any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException and log failure when exit pass is invalid")
        void shouldThrowWhenExitPassInvalid() {
            // Arrange
            GateExitRequest request = buildExitRequest();
            Vehicle vehicle = buildActiveVehicle();
            GateSession openSession = buildOpenSession();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.of(openSession));
            when(exitPassService.validateExitPass(EXIT_PASS_TOKEN, VEHICLE_ID, TENANT_ID))
                    .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> gateService.processExit(request))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("Invalid or expired exit pass token");

            // Verify failure event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog failLog = logCaptor.getValue();
            assertThat(failLog.getEventType()).isEqualTo(GateEventType.EXIT_PASS_FAILED);
            assertThat(failLog.isSuccess()).isFalse();
            assertThat(failLog.getSessionId()).isEqualTo(SESSION_ID);

            // Verify session was NOT updated
            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle missing resident gracefully on exit")
        void shouldHandleMissingResidentOnExit() {
            // Arrange
            GateExitRequest request = buildExitRequest();
            Vehicle vehicle = buildActiveVehicle();
            GateSession openSession = buildOpenSession();

            when(vehicleRepository.findByQrStickerCodeAndTenantId(QR_STICKER_CODE, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(gateSessionRepository.findByVehicleIdAndStatusAndTenantId(VEHICLE_ID, GateSessionStatus.OPEN, TENANT_ID))
                    .thenReturn(Optional.of(openSession));
            when(exitPassService.validateExitPass(EXIT_PASS_TOKEN, VEHICLE_ID, TENANT_ID))
                    .thenReturn(true);
            when(gateSessionRepository.save(any(GateSession.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act
            GateExitResponse response = gateService.processExit(request);

            // Assert - resident fields should be null when resident not found
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(GateSessionStatus.CLOSED);
            assertThat(response.getResidentFirstName()).isNull();
            assertThat(response.getResidentLastName()).isNull();
        }
    }

    // ---------------------------------------------------------------
    // getSession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("should return session response when session exists")
        void shouldReturnSessionResponse() {
            // Arrange
            GateSession session = buildOpenSession();
            GateSessionResponse expectedResponse = buildSessionResponse(session);

            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.of(session));
            when(gateSessionMapper.toResponse(session))
                    .thenReturn(expectedResponse);

            // Act
            GateSessionResponse response = gateService.getSession(SESSION_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(SESSION_ID);
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(response.getPlateNumber()).isEqualTo(PLATE_NUMBER);
            assertThat(response.getStatus()).isEqualTo(GateSessionStatus.OPEN);
            assertThat(response.getEntryGuardId()).isEqualTo(GUARD_ID);

            verify(gateSessionRepository).findByIdAndTenantId(SESSION_ID, TENANT_ID);
            verify(gateSessionMapper).toResponse(session);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void shouldThrowWhenSessionNotFound() {
            // Arrange
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(gateSessionRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.getSession(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("GateSession")
                    .hasMessageContaining(nonExistentId.toString());

            verify(gateSessionMapper, never()).toResponse(any());
        }
    }

    // ---------------------------------------------------------------
    // getOpenSessions
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getOpenSessions")
    class GetOpenSessions {

        @Test
        @DisplayName("should return paged open sessions")
        void shouldReturnPagedOpenSessions() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            GateSession session1 = buildOpenSession();
            GateSession session2 = buildOpenSession();
            session2.setId(UUID.fromString("00000000-0000-0000-0000-000000000005"));

            List<GateSession> sessions = List.of(session1, session2);
            Page<GateSession> page = new PageImpl<>(sessions, pageable, 2);

            GateSessionResponse response1 = buildSessionResponse(session1);
            GateSessionResponse response2 = buildSessionResponse(session2);

            when(gateSessionRepository.findByStatusAndTenantId(GateSessionStatus.OPEN, TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateSessionMapper.toResponse(session1)).thenReturn(response1);
            when(gateSessionMapper.toResponse(session2)).thenReturn(response2);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getOpenSessions(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(gateSessionRepository).findByStatusAndTenantId(GateSessionStatus.OPEN, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty paged response when no open sessions exist")
        void shouldReturnEmptyPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<GateSession> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(gateSessionRepository.findByStatusAndTenantId(GateSessionStatus.OPEN, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getOpenSessions(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }
    }

    // ---------------------------------------------------------------
    // getAllSessions
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAllSessions")
    class GetAllSessions {

        @Test
        @DisplayName("should return paged sessions for tenant")
        void shouldReturnPagedSessions() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            GateSession openSession = buildOpenSession();
            GateSession closedSession = buildClosedSession();
            closedSession.setId(UUID.fromString("00000000-0000-0000-0000-000000000006"));

            List<GateSession> sessions = List.of(openSession, closedSession);
            Page<GateSession> page = new PageImpl<>(sessions, pageable, 2);

            GateSessionResponse openResponse = buildSessionResponse(openSession);
            GateSessionResponse closedResponse = buildSessionResponse(closedSession);

            when(gateSessionRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateSessionMapper.toResponse(openSession)).thenReturn(openResponse);
            when(gateSessionMapper.toResponse(closedSession)).thenReturn(closedResponse);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getAllSessions(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(gateSessionRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty paged response when no sessions exist")
        void shouldReturnEmptyPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<GateSession> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(gateSessionRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getAllSessions(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should handle pagination correctly for second page")
        void shouldHandlePaginationForSecondPage() {
            // Arrange
            Pageable pageable = PageRequest.of(1, 5);
            GateSession session = buildOpenSession();
            List<GateSession> sessions = List.of(session);
            Page<GateSession> page = new PageImpl<>(sessions, pageable, 8);

            GateSessionResponse response = buildSessionResponse(session);
            when(gateSessionRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateSessionMapper.toResponse(session)).thenReturn(response);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getAllSessions(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.isFirst()).isFalse();
            assertThat(result.isLast()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // getAllAccessLogs
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAllAccessLogs")
    class GetAllAccessLogs {

        @Test
        @DisplayName("should return paged access logs for tenant")
        void shouldReturnPagedAccessLogs() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            GateAccessLog log1 = new GateAccessLog();
            log1.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
            log1.setTenantId(TENANT_ID);
            log1.setSessionId(SESSION_ID);
            log1.setVehicleId(VEHICLE_ID);
            log1.setResidentId(RESIDENT_ID);
            log1.setEventType(GateEventType.ENTRY_SCAN);
            log1.setGuardId(GUARD_ID);
            log1.setDetails("Vehicle entered gate");
            log1.setSuccess(true);

            GateAccessLog log2 = new GateAccessLog();
            log2.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
            log2.setTenantId(TENANT_ID);
            log2.setSessionId(SESSION_ID);
            log2.setVehicleId(VEHICLE_ID);
            log2.setResidentId(RESIDENT_ID);
            log2.setEventType(GateEventType.EXIT_SCAN);
            log2.setGuardId(GUARD_ID);
            log2.setDetails("Vehicle exited gate");
            log2.setSuccess(true);

            List<GateAccessLog> logs = List.of(log1, log2);
            Page<GateAccessLog> page = new PageImpl<>(logs, pageable, 2);

            GateAccessLogResponse response1 = GateAccessLogResponse.builder()
                    .id(log1.getId())
                    .sessionId(SESSION_ID)
                    .vehicleId(VEHICLE_ID)
                    .residentId(RESIDENT_ID)
                    .eventType(GateEventType.ENTRY_SCAN)
                    .guardId(GUARD_ID)
                    .details("Vehicle entered gate")
                    .success(true)
                    .build();

            GateAccessLogResponse response2 = GateAccessLogResponse.builder()
                    .id(log2.getId())
                    .sessionId(SESSION_ID)
                    .vehicleId(VEHICLE_ID)
                    .residentId(RESIDENT_ID)
                    .eventType(GateEventType.EXIT_SCAN)
                    .guardId(GUARD_ID)
                    .details("Vehicle exited gate")
                    .success(true)
                    .build();

            when(gateAccessLogRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateAccessLogMapper.toResponse(log1)).thenReturn(response1);
            when(gateAccessLogMapper.toResponse(log2)).thenReturn(response2);

            // Act
            PagedResponse<GateAccessLogResponse> result = gateService.getAllAccessLogs(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getEventType()).isEqualTo(GateEventType.ENTRY_SCAN);
            assertThat(result.getContent().get(1).getEventType()).isEqualTo(GateEventType.EXIT_SCAN);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(gateAccessLogRepository).findAllByTenantId(TENANT_ID, pageable);
            verify(gateAccessLogMapper).toResponse(log1);
            verify(gateAccessLogMapper).toResponse(log2);
        }

        @Test
        @DisplayName("should return empty paged response when no access logs exist")
        void shouldReturnEmptyPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<GateAccessLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(gateAccessLogRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            // Act
            PagedResponse<GateAccessLogResponse> result = gateService.getAllAccessLogs(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }
    }

    // ---------------------------------------------------------------
    // processRemoteApprovalExit
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("processRemoteApprovalExit")
    class ProcessRemoteApprovalExit {

        @Test
        @DisplayName("should close session via remote approval and log exit event")
        void shouldCloseSessionViaRemoteApproval() {
            // Arrange
            GateSession openSession = buildOpenSession();
            Vehicle vehicle = buildActiveVehicle();
            Resident resident = buildResident();

            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.of(openSession));
            when(gateSessionRepository.save(any(GateSession.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            // Act
            GateExitResponse response = gateService.processRemoteApprovalExit(SESSION_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(response.getStatus()).isEqualTo(GateSessionStatus.CLOSED);
            assertThat(response.getExitTime()).isNotNull();
            assertThat(response.getResidentFirstName()).isEqualTo("John");
            assertThat(response.getResidentLastName()).isEqualTo("Doe");

            // Verify session was updated
            ArgumentCaptor<GateSession> sessionCaptor = ArgumentCaptor.forClass(GateSession.class);
            verify(gateSessionRepository).save(sessionCaptor.capture());
            GateSession closedSession = sessionCaptor.getValue();
            assertThat(closedSession.getStatus()).isEqualTo(GateSessionStatus.CLOSED);
            assertThat(closedSession.getExitGuardId()).isEqualTo(GUARD_ID);

            // Verify exit log
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog exitLog = logCaptor.getValue();
            assertThat(exitLog.getEventType()).isEqualTo(GateEventType.EXIT_SCAN);
            assertThat(exitLog.isSuccess()).isTrue();
            assertThat(exitLog.getDetails()).contains("remote approval");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void shouldThrowWhenSessionNotFound() {
            // Arrange
            UUID unknownSessionId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(gateSessionRepository.findByIdAndTenantId(unknownSessionId, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> gateService.processRemoteApprovalExit(unknownSessionId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("GateSession");

            verify(gateSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when session is not open")
        void shouldThrowWhenSessionNotOpen() {
            // Arrange
            GateSession closedSession = buildClosedSession();
            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.of(closedSession));

            // Act & Assert
            assertThatThrownBy(() -> gateService.processRemoteApprovalExit(SESSION_ID))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("not open");

            verify(gateSessionRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getSessionsByVehicle
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getSessionsByVehicle")
    class GetSessionsByVehicle {

        @Test
        @DisplayName("should return paged sessions for a specific vehicle")
        void shouldReturnPagedSessionsForVehicle() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            GateSession session = buildOpenSession();
            List<GateSession> sessions = List.of(session);
            Page<GateSession> page = new PageImpl<>(sessions, pageable, 1);

            GateSessionResponse expectedResponse = buildSessionResponse(session);

            when(gateSessionRepository.findByVehicleIdAndTenantId(VEHICLE_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateSessionMapper.toResponse(session)).thenReturn(expectedResponse);

            // Act
            PagedResponse<GateSessionResponse> result = gateService.getSessionsByVehicle(VEHICLE_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(gateSessionRepository).findByVehicleIdAndTenantId(VEHICLE_ID, TENANT_ID, pageable);
        }
    }

    // ---------------------------------------------------------------
    // getAccessLogsBySession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAccessLogsBySession")
    class GetAccessLogsBySession {

        @Test
        @DisplayName("should return paged access logs for a specific session")
        void shouldReturnPagedAccessLogsForSession() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            GateAccessLog log = new GateAccessLog();
            log.setId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
            log.setTenantId(TENANT_ID);
            log.setSessionId(SESSION_ID);
            log.setVehicleId(VEHICLE_ID);
            log.setResidentId(RESIDENT_ID);
            log.setEventType(GateEventType.ENTRY_SCAN);
            log.setGuardId(GUARD_ID);
            log.setSuccess(true);

            Page<GateAccessLog> page = new PageImpl<>(List.of(log), pageable, 1);

            GateAccessLogResponse expectedResponse = buildAccessLogResponse();

            when(gateAccessLogRepository.findBySessionIdAndTenantId(SESSION_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateAccessLogMapper.toResponse(log)).thenReturn(expectedResponse);

            // Act
            PagedResponse<GateAccessLogResponse> result = gateService.getAccessLogsBySession(SESSION_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(gateAccessLogRepository).findBySessionIdAndTenantId(SESSION_ID, TENANT_ID, pageable);
        }
    }

    // ---------------------------------------------------------------
    // getAccessLogsByVehicle
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAccessLogsByVehicle")
    class GetAccessLogsByVehicle {

        @Test
        @DisplayName("should return paged access logs for a specific vehicle")
        void shouldReturnPagedAccessLogsForVehicle() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            GateAccessLog log = new GateAccessLog();
            log.setId(UUID.fromString("00000000-0000-0000-0000-000000000013"));
            log.setTenantId(TENANT_ID);
            log.setSessionId(SESSION_ID);
            log.setVehicleId(VEHICLE_ID);
            log.setResidentId(RESIDENT_ID);
            log.setEventType(GateEventType.EXIT_SCAN);
            log.setGuardId(GUARD_ID);
            log.setSuccess(true);

            Page<GateAccessLog> page = new PageImpl<>(List.of(log), pageable, 1);

            GateAccessLogResponse expectedResponse = buildAccessLogResponse();

            when(gateAccessLogRepository.findByVehicleIdAndTenantId(VEHICLE_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(gateAccessLogMapper.toResponse(log)).thenReturn(expectedResponse);

            // Act
            PagedResponse<GateAccessLogResponse> result = gateService.getAccessLogsByVehicle(VEHICLE_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(gateAccessLogRepository).findByVehicleIdAndTenantId(VEHICLE_ID, TENANT_ID, pageable);
        }
    }
}
