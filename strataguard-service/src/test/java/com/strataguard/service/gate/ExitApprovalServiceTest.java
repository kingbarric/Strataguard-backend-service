package com.strataguard.service.gate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.approval.CreateExitApprovalRequest;
import com.strataguard.core.dto.approval.ExitApprovalResponse;
import com.strataguard.core.entity.ExitApprovalRequest;
import com.strataguard.core.entity.GateAccessLog;
import com.strataguard.core.entity.GateSession;
import com.strataguard.core.entity.Vehicle;
import com.strataguard.core.enums.ExitApprovalStatus;
import com.strataguard.core.enums.GateEventType;
import com.strataguard.core.enums.GateSessionStatus;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import com.strataguard.core.exception.GateAccessDeniedException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ExitApprovalMapper;
import com.strataguard.infrastructure.repository.ExitApprovalRequestRepository;
import com.strataguard.infrastructure.repository.GateAccessLogRepository;
import com.strataguard.infrastructure.repository.GateSessionRepository;
import com.strataguard.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitApprovalService")
class ExitApprovalServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID APPROVAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final String GUARD_ID = "guard-user-id";
    private static final int EXPIRY_MINUTES = 5;

    @Mock
    private ExitApprovalRequestRepository approvalRepository;

    @Mock
    private GateSessionRepository gateSessionRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private GateAccessLogRepository gateAccessLogRepository;

    @Mock
    private ExitApprovalMapper exitApprovalMapper;

    private ExitApprovalService exitApprovalService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(GUARD_ID);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        exitApprovalService = new ExitApprovalService(
                approvalRepository, gateSessionRepository, vehicleRepository,
                gateAccessLogRepository, exitApprovalMapper, EXPIRY_MINUTES);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------
    // Helper builders
    // ---------------------------------------------------------------

    private GateSession buildOpenSession() {
        GateSession session = new GateSession();
        session.setId(SESSION_ID);
        session.setTenantId(TENANT_ID);
        session.setVehicleId(VEHICLE_ID);
        session.setResidentId(RESIDENT_ID);
        session.setPlateNumber("KAA 123A");
        session.setStatus(GateSessionStatus.OPEN);
        session.setEntryTime(Instant.parse("2026-02-17T08:00:00Z"));
        session.setEntryGuardId(GUARD_ID);
        return session;
    }

    private GateSession buildClosedSession() {
        GateSession session = buildOpenSession();
        session.setStatus(GateSessionStatus.CLOSED);
        session.setExitTime(Instant.parse("2026-02-17T17:00:00Z"));
        session.setExitGuardId(GUARD_ID);
        return session;
    }

    private Vehicle buildVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setTenantId(TENANT_ID);
        vehicle.setResidentId(RESIDENT_ID);
        vehicle.setPlateNumber("KAA 123A");
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        return vehicle;
    }

    private CreateExitApprovalRequest buildCreateRequest() {
        CreateExitApprovalRequest request = new CreateExitApprovalRequest();
        request.setSessionId(SESSION_ID);
        request.setNote("Please approve exit for delivery vehicle");
        return request;
    }

    private ExitApprovalRequest buildPendingApproval() {
        ExitApprovalRequest approval = new ExitApprovalRequest();
        approval.setId(APPROVAL_ID);
        approval.setTenantId(TENANT_ID);
        approval.setSessionId(SESSION_ID);
        approval.setVehicleId(VEHICLE_ID);
        approval.setResidentId(RESIDENT_ID);
        approval.setGuardId(GUARD_ID);
        approval.setStatus(ExitApprovalStatus.PENDING);
        approval.setExpiresAt(Instant.now().plusSeconds(EXPIRY_MINUTES * 60L));
        approval.setNote("Please approve exit for delivery vehicle");
        return approval;
    }

    private ExitApprovalRequest buildExpiredApproval() {
        ExitApprovalRequest approval = buildPendingApproval();
        approval.setExpiresAt(Instant.now().minusSeconds(60));
        return approval;
    }

    private ExitApprovalResponse buildApprovalResponse(ExitApprovalRequest approval) {
        return ExitApprovalResponse.builder()
                .id(approval.getId())
                .sessionId(approval.getSessionId())
                .vehicleId(approval.getVehicleId())
                .residentId(approval.getResidentId())
                .guardId(approval.getGuardId())
                .status(approval.getStatus())
                .expiresAt(approval.getExpiresAt())
                .respondedAt(approval.getRespondedAt())
                .note(approval.getNote())
                .build();
    }

    // ---------------------------------------------------------------
    // createApprovalRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createApprovalRequest")
    class CreateApprovalRequest {

        @Test
        @DisplayName("should create approval request with PENDING status and enrich with vehicle details")
        void shouldCreateApprovalRequestSuccessfully() {
            // Arrange
            CreateExitApprovalRequest request = buildCreateRequest();
            GateSession session = buildOpenSession();
            Vehicle vehicle = buildVehicle();

            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.of(session));
            when(approvalRepository.save(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> {
                        ExitApprovalRequest saved = invocation.getArgument(0);
                        saved.setId(APPROVAL_ID);
                        return saved;
                    });
            when(exitApprovalMapper.toResponse(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> {
                        ExitApprovalRequest a = invocation.getArgument(0);
                        return buildApprovalResponse(a);
                    });
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitApprovalResponse response = exitApprovalService.createApprovalRequest(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(response.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(response.getStatus()).isEqualTo(ExitApprovalStatus.PENDING);
            assertThat(response.getPlateNumber()).isEqualTo("KAA 123A");
            assertThat(response.getVehicleMake()).isEqualTo("Toyota");
            assertThat(response.getVehicleModel()).isEqualTo("Corolla");
            assertThat(response.getVehicleColor()).isEqualTo("White");

            // Verify approval entity was saved with correct fields
            ArgumentCaptor<ExitApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ExitApprovalRequest.class);
            verify(approvalRepository).save(approvalCaptor.capture());
            ExitApprovalRequest savedApproval = approvalCaptor.getValue();
            assertThat(savedApproval.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(savedApproval.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(savedApproval.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(savedApproval.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(savedApproval.getGuardId()).isEqualTo(GUARD_ID);
            assertThat(savedApproval.getStatus()).isEqualTo(ExitApprovalStatus.PENDING);
            assertThat(savedApproval.getExpiresAt()).isAfter(Instant.now());
            assertThat(savedApproval.getNote()).isEqualTo("Please approve exit for delivery vehicle");

            // Verify REMOTE_APPROVAL_REQUESTED event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getEventType()).isEqualTo(GateEventType.REMOTE_APPROVAL_REQUESTED);
            assertThat(savedLog.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(savedLog.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(savedLog.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(savedLog.getGuardId()).isEqualTo(GUARD_ID);
            assertThat(savedLog.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session not found")
        void shouldThrowWhenSessionNotFound() {
            // Arrange
            CreateExitApprovalRequest request = buildCreateRequest();
            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.createApprovalRequest(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("GateSession")
                    .hasMessageContaining("id");

            verify(approvalRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when session is not OPEN")
        void shouldThrowWhenSessionNotOpen() {
            // Arrange
            CreateExitApprovalRequest request = buildCreateRequest();
            GateSession closedSession = buildClosedSession();

            when(gateSessionRepository.findByIdAndTenantId(SESSION_ID, TENANT_ID))
                    .thenReturn(Optional.of(closedSession));

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.createApprovalRequest(request))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("Session is not open")
                    .hasMessageContaining("CLOSED");

            verify(approvalRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getPendingApprovalsForResident
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getPendingApprovalsForResident")
    class GetPendingApprovalsForResident {

        @Test
        @DisplayName("should return non-expired pending approvals enriched with vehicle details")
        void shouldReturnNonExpiredApprovals() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            Vehicle vehicle = buildVehicle();
            ExitApprovalResponse mappedResponse = buildApprovalResponse(approval);

            when(approvalRepository.findByResidentIdAndStatusAndTenantId(RESIDENT_ID, ExitApprovalStatus.PENDING, TENANT_ID))
                    .thenReturn(List.of(approval));
            when(exitApprovalMapper.toResponse(approval)).thenReturn(mappedResponse);
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            List<ExitApprovalResponse> results = exitApprovalService.getPendingApprovalsForResident(RESIDENT_ID);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(APPROVAL_ID);
            assertThat(results.get(0).getPlateNumber()).isEqualTo("KAA 123A");
            assertThat(results.get(0).getVehicleMake()).isEqualTo("Toyota");
            assertThat(results.get(0).getVehicleModel()).isEqualTo("Corolla");
            assertThat(results.get(0).getVehicleColor()).isEqualTo("White");

            verify(approvalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should filter out expired approvals and mark them as EXPIRED")
        void shouldFilterOutExpiredApprovals() {
            // Arrange
            ExitApprovalRequest expiredApproval = buildExpiredApproval();
            ExitApprovalRequest validApproval = buildPendingApproval();
            validApproval.setId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            Vehicle vehicle = buildVehicle();
            ExitApprovalResponse validResponse = buildApprovalResponse(validApproval);

            when(approvalRepository.findByResidentIdAndStatusAndTenantId(RESIDENT_ID, ExitApprovalStatus.PENDING, TENANT_ID))
                    .thenReturn(List.of(expiredApproval, validApproval));
            when(exitApprovalMapper.toResponse(validApproval)).thenReturn(validResponse);
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            List<ExitApprovalResponse> results = exitApprovalService.getPendingApprovalsForResident(RESIDENT_ID);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(validApproval.getId());

            // Verify expired approval was saved with EXPIRED status
            assertThat(expiredApproval.getStatus()).isEqualTo(ExitApprovalStatus.EXPIRED);
            verify(approvalRepository).save(expiredApproval);

            // Verify REMOTE_APPROVAL_EXPIRED event was logged for the expired one
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog expiredLog = logCaptor.getValue();
            assertThat(expiredLog.getEventType()).isEqualTo(GateEventType.REMOTE_APPROVAL_EXPIRED);
            assertThat(expiredLog.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no pending approvals exist")
        void shouldReturnEmptyListWhenNoPendingApprovals() {
            // Arrange
            when(approvalRepository.findByResidentIdAndStatusAndTenantId(RESIDENT_ID, ExitApprovalStatus.PENDING, TENANT_ID))
                    .thenReturn(List.of());

            // Act
            List<ExitApprovalResponse> results = exitApprovalService.getPendingApprovalsForResident(RESIDENT_ID);

            // Assert
            assertThat(results).isEmpty();

            verify(approvalRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // approveRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("approveRequest")
    class ApproveRequest {

        @Test
        @DisplayName("should approve pending request and log REMOTE_APPROVAL_APPROVED event")
        void shouldApprovePendingRequest() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            Vehicle vehicle = buildVehicle();

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));
            when(approvalRepository.save(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(exitApprovalMapper.toResponse(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> {
                        ExitApprovalRequest a = invocation.getArgument(0);
                        return buildApprovalResponse(a);
                    });
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitApprovalResponse response = exitApprovalService.approveRequest(APPROVAL_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ExitApprovalStatus.APPROVED);
            assertThat(response.getRespondedAt()).isNotNull();
            assertThat(response.getPlateNumber()).isEqualTo("KAA 123A");

            // Verify entity was updated
            assertThat(approval.getStatus()).isEqualTo(ExitApprovalStatus.APPROVED);
            assertThat(approval.getRespondedAt()).isNotNull();

            // Verify REMOTE_APPROVAL_APPROVED event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getEventType()).isEqualTo(GateEventType.REMOTE_APPROVAL_APPROVED);
            assertThat(savedLog.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(savedLog.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when approval not found")
        void shouldThrowWhenApprovalNotFound() {
            // Arrange
            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.approveRequest(APPROVAL_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ExitApprovalRequest")
                    .hasMessageContaining("id");

            verify(approvalRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when approval is not PENDING")
        void shouldThrowWhenApprovalNotPending() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            approval.setStatus(ExitApprovalStatus.APPROVED);

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.approveRequest(APPROVAL_ID))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("not pending")
                    .hasMessageContaining("APPROVED");

            verify(gateAccessLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should mark as EXPIRED and throw GateAccessDeniedException when approval has expired")
        void shouldMarkExpiredAndThrowWhenApprovalExpired() {
            // Arrange
            ExitApprovalRequest approval = buildExpiredApproval();

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.approveRequest(APPROVAL_ID))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("expired");

            // Verify it was saved as EXPIRED
            assertThat(approval.getStatus()).isEqualTo(ExitApprovalStatus.EXPIRED);
            verify(approvalRepository).save(approval);

            // Verify REMOTE_APPROVAL_EXPIRED event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getEventType()).isEqualTo(GateEventType.REMOTE_APPROVAL_EXPIRED);
            assertThat(savedLog.isSuccess()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // denyRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("denyRequest")
    class DenyRequest {

        @Test
        @DisplayName("should deny pending request and log REMOTE_APPROVAL_DENIED event")
        void shouldDenyPendingRequest() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            Vehicle vehicle = buildVehicle();

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));
            when(approvalRepository.save(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(exitApprovalMapper.toResponse(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> {
                        ExitApprovalRequest a = invocation.getArgument(0);
                        return buildApprovalResponse(a);
                    });
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitApprovalResponse response = exitApprovalService.denyRequest(APPROVAL_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ExitApprovalStatus.DENIED);
            assertThat(response.getRespondedAt()).isNotNull();

            // Verify entity was updated
            assertThat(approval.getStatus()).isEqualTo(ExitApprovalStatus.DENIED);
            assertThat(approval.getRespondedAt()).isNotNull();

            // Verify REMOTE_APPROVAL_DENIED event was logged
            ArgumentCaptor<GateAccessLog> logCaptor = ArgumentCaptor.forClass(GateAccessLog.class);
            verify(gateAccessLogRepository).save(logCaptor.capture());
            GateAccessLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getEventType()).isEqualTo(GateEventType.REMOTE_APPROVAL_DENIED);
            assertThat(savedLog.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(savedLog.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when approval not found")
        void shouldThrowWhenApprovalNotFound() {
            // Arrange
            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.denyRequest(APPROVAL_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ExitApprovalRequest")
                    .hasMessageContaining("id");

            verify(approvalRepository, never()).save(any());
            verify(gateAccessLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw GateAccessDeniedException when approval is not PENDING")
        void shouldThrowWhenApprovalNotPending() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            approval.setStatus(ExitApprovalStatus.DENIED);

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.denyRequest(APPROVAL_ID))
                    .isInstanceOf(GateAccessDeniedException.class)
                    .hasMessageContaining("not pending")
                    .hasMessageContaining("DENIED");

            verify(gateAccessLogRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getApprovalStatus
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getApprovalStatus")
    class GetApprovalStatus {

        @Test
        @DisplayName("should return approval status enriched with vehicle details")
        void shouldReturnApprovalStatus() {
            // Arrange
            ExitApprovalRequest approval = buildPendingApproval();
            Vehicle vehicle = buildVehicle();
            ExitApprovalResponse mappedResponse = buildApprovalResponse(approval);

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));
            when(exitApprovalMapper.toResponse(approval)).thenReturn(mappedResponse);
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitApprovalResponse response = exitApprovalService.getApprovalStatus(APPROVAL_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(APPROVAL_ID);
            assertThat(response.getStatus()).isEqualTo(ExitApprovalStatus.PENDING);
            assertThat(response.getPlateNumber()).isEqualTo("KAA 123A");
            assertThat(response.getVehicleMake()).isEqualTo("Toyota");

            // Should NOT save since it is not expired
            verify(approvalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should mark PENDING approval as EXPIRED when past expiry time")
        void shouldMarkPendingAsExpiredWhenPastExpiry() {
            // Arrange
            ExitApprovalRequest approval = buildExpiredApproval();
            Vehicle vehicle = buildVehicle();

            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(approval));
            when(exitApprovalMapper.toResponse(any(ExitApprovalRequest.class)))
                    .thenAnswer(invocation -> {
                        ExitApprovalRequest a = invocation.getArgument(0);
                        return buildApprovalResponse(a);
                    });
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            // Act
            ExitApprovalResponse response = exitApprovalService.getApprovalStatus(APPROVAL_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(ExitApprovalStatus.EXPIRED);

            // Verify entity was saved with EXPIRED status
            assertThat(approval.getStatus()).isEqualTo(ExitApprovalStatus.EXPIRED);
            verify(approvalRepository).save(approval);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when approval not found")
        void shouldThrowWhenApprovalNotFound() {
            // Arrange
            when(approvalRepository.findByIdAndTenantId(APPROVAL_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> exitApprovalService.getApprovalStatus(APPROVAL_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ExitApprovalRequest")
                    .hasMessageContaining("id");

            verify(exitApprovalMapper, never()).toResponse(any());
        }
    }
}
