package com.strataguard.api.controller;

import com.strataguard.core.dto.approval.CreateExitApprovalRequest;
import com.strataguard.core.dto.approval.ExitApprovalResponse;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.enums.ExitApprovalStatus;
import com.strataguard.service.gate.ExitApprovalService;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitApprovalController")
class ExitApprovalControllerTest {

    private static final UUID APPROVAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private ExitApprovalService exitApprovalService;

    @InjectMocks
    private ExitApprovalController exitApprovalController;

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private ExitApprovalResponse buildApprovalResponse(ExitApprovalStatus status) {
        return ExitApprovalResponse.builder()
                .id(APPROVAL_ID)
                .sessionId(SESSION_ID)
                .vehicleId(VEHICLE_ID)
                .residentId(RESIDENT_ID)
                .guardId("guard-user-id")
                .status(status)
                .expiresAt(Instant.parse("2026-02-17T08:10:00Z"))
                .note("Please approve exit")
                .plateNumber("KAA 123A")
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .vehicleColor("White")
                .createdAt(Instant.parse("2026-02-17T08:00:00Z"))
                .updatedAt(Instant.parse("2026-02-17T08:00:00Z"))
                .build();
    }

    // ---------------------------------------------------------------
    // createApprovalRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST / - createApprovalRequest")
    class CreateApprovalRequest {

        @Test
        @DisplayName("should return 201 CREATED with approval response and success message")
        void shouldReturn201WithApprovalResponse() {
            // Arrange
            CreateExitApprovalRequest request = new CreateExitApprovalRequest();
            request.setSessionId(SESSION_ID);
            request.setNote("Please approve exit");

            ExitApprovalResponse serviceResponse = buildApprovalResponse(ExitApprovalStatus.PENDING);

            when(exitApprovalService.createApprovalRequest(request)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<ExitApprovalResponse>> result =
                    exitApprovalController.createApprovalRequest(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Exit approval request created");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(APPROVAL_ID);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(ExitApprovalStatus.PENDING);

            verify(exitApprovalService).createApprovalRequest(request);
        }
    }

    // ---------------------------------------------------------------
    // getPendingApprovals
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /pending - getPendingApprovals")
    class GetPendingApprovals {

        @Test
        @DisplayName("should return 200 OK with list of pending approvals for resident")
        void shouldReturn200WithPendingApprovals() {
            // Arrange
            ExitApprovalResponse pendingApproval = buildApprovalResponse(ExitApprovalStatus.PENDING);
            List<ExitApprovalResponse> serviceResponse = List.of(pendingApproval);

            when(exitApprovalService.getPendingApprovalsForResident(RESIDENT_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<List<ExitApprovalResponse>>> result =
                    exitApprovalController.getPendingApprovals(RESIDENT_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData()).hasSize(1);
            assertThat(result.getBody().getData().get(0).getStatus()).isEqualTo(ExitApprovalStatus.PENDING);
            assertThat(result.getBody().getData().get(0).getResidentId()).isEqualTo(RESIDENT_ID);

            verify(exitApprovalService).getPendingApprovalsForResident(RESIDENT_ID);
        }

        @Test
        @DisplayName("should return 200 OK with empty list when no pending approvals exist")
        void shouldReturn200WithEmptyListWhenNoPendingApprovals() {
            // Arrange
            when(exitApprovalService.getPendingApprovalsForResident(RESIDENT_ID)).thenReturn(List.of());

            // Act
            ResponseEntity<ApiResponse<List<ExitApprovalResponse>>> result =
                    exitApprovalController.getPendingApprovals(RESIDENT_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEmpty();

            verify(exitApprovalService).getPendingApprovalsForResident(RESIDENT_ID);
        }
    }

    // ---------------------------------------------------------------
    // approveRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /{id}/approve - approveRequest")
    class ApproveRequest {

        @Test
        @DisplayName("should return 200 OK with approved response and success message")
        void shouldReturn200WithApprovedResponse() {
            // Arrange
            ExitApprovalResponse serviceResponse = buildApprovalResponse(ExitApprovalStatus.APPROVED);
            serviceResponse.setRespondedAt(Instant.parse("2026-02-17T08:05:00Z"));

            when(exitApprovalService.approveRequest(APPROVAL_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<ExitApprovalResponse>> result =
                    exitApprovalController.approveRequest(APPROVAL_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Exit approval approved");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(ExitApprovalStatus.APPROVED);
            assertThat(result.getBody().getData().getRespondedAt()).isNotNull();

            verify(exitApprovalService).approveRequest(APPROVAL_ID);
        }
    }

    // ---------------------------------------------------------------
    // denyRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /{id}/deny - denyRequest")
    class DenyRequest {

        @Test
        @DisplayName("should return 200 OK with denied response and success message")
        void shouldReturn200WithDeniedResponse() {
            // Arrange
            ExitApprovalResponse serviceResponse = buildApprovalResponse(ExitApprovalStatus.DENIED);
            serviceResponse.setRespondedAt(Instant.parse("2026-02-17T08:05:00Z"));

            when(exitApprovalService.denyRequest(APPROVAL_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<ExitApprovalResponse>> result =
                    exitApprovalController.denyRequest(APPROVAL_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Exit approval denied");
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(ExitApprovalStatus.DENIED);
            assertThat(result.getBody().getData().getRespondedAt()).isNotNull();

            verify(exitApprovalService).denyRequest(APPROVAL_ID);
        }
    }

    // ---------------------------------------------------------------
    // getApprovalStatus
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /{id}/status - getApprovalStatus")
    class GetApprovalStatus {

        @Test
        @DisplayName("should return 200 OK with approval status response")
        void shouldReturn200WithApprovalStatusResponse() {
            // Arrange
            ExitApprovalResponse serviceResponse = buildApprovalResponse(ExitApprovalStatus.PENDING);

            when(exitApprovalService.getApprovalStatus(APPROVAL_ID)).thenReturn(serviceResponse);

            // Act
            ResponseEntity<ApiResponse<ExitApprovalResponse>> result =
                    exitApprovalController.getApprovalStatus(APPROVAL_ID);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(serviceResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(APPROVAL_ID);
            assertThat(result.getBody().getData().getStatus()).isEqualTo(ExitApprovalStatus.PENDING);

            verify(exitApprovalService).getApprovalStatus(APPROVAL_ID);
        }
    }
}
