package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.visitor.*;
import com.strataguard.core.enums.VisitPassStatus;
import com.strataguard.core.enums.VisitPassType;
import com.strataguard.core.enums.VisitorStatus;
import com.strataguard.core.enums.VisitorType;
import com.strataguard.service.visitor.VisitorService;
import org.junit.jupiter.api.BeforeEach;
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
class VisitorControllerTest {

    @Mock
    private VisitorService visitorService;

    @InjectMocks
    private VisitorController visitorController;

    private static final UUID VISITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INVITED_BY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private VisitorResponse visitorResponse;
    private VisitPassResponse visitPassResponse;
    private PagedResponse<VisitorResponse> pagedResponse;

    @BeforeEach
    void setUp() {
        visitorResponse = VisitorResponse.builder()
                .id(VISITOR_ID)
                .name("Jane Doe")
                .phone("+254712345678")
                .email("jane@example.com")
                .purpose("Delivery")
                .invitedBy(INVITED_BY_ID)
                .invitedByName("John Resident")
                .visitorType(VisitorType.PEDESTRIAN)
                .vehiclePlateNumber("KAA123A")
                .status(VisitorStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        visitPassResponse = VisitPassResponse.builder()
                .id(PASS_ID)
                .visitorId(VISITOR_ID)
                .passCode("pass-code-123")
                .qrData("base64-qr-data")
                .token("jwt-token")
                .verificationCode("123456")
                .passType(VisitPassType.SINGLE_USE)
                .validFrom(Instant.now())
                .validTo(Instant.now().plusSeconds(86400))
                .maxEntries(1)
                .usedEntries(0)
                .status(VisitPassStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        pagedResponse = PagedResponse.<VisitorResponse>builder()
                .content(List.of(visitorResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    // ========================================================================
    // createVisitor
    // ========================================================================

    @Nested
    @DisplayName("POST / - createVisitor")
    class CreateVisitor {

        private CreateVisitorRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateVisitorRequest.builder()
                    .name("Jane Doe")
                    .phone("+254712345678")
                    .email("jane@example.com")
                    .purpose("Delivery")
                    .visitorType(VisitorType.PEDESTRIAN)
                    .passType(VisitPassType.SINGLE_USE)
                    .validFrom(Instant.now())
                    .validTo(Instant.now().plusSeconds(86400))
                    .maxEntries(1)
                    .build();
        }

        @Test
        @DisplayName("should return 201 CREATED with success message")
        void shouldReturnCreatedStatus() {
            when(visitorService.createVisitor(createRequest)).thenReturn(visitorResponse);

            ResponseEntity<ApiResponse<VisitorResponse>> result = visitorController.createVisitor(createRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visitor invitation created successfully");
            assertThat(result.getBody().getData()).isEqualTo(visitorResponse);

            verify(visitorService).createVisitor(createRequest);
        }

        @Test
        @DisplayName("should delegate to visitorService.createVisitor")
        void shouldDelegateToService() {
            when(visitorService.createVisitor(createRequest)).thenReturn(visitorResponse);

            visitorController.createVisitor(createRequest);

            verify(visitorService).createVisitor(createRequest);
        }
    }

    // ========================================================================
    // getAllVisitors
    // ========================================================================

    @Nested
    @DisplayName("GET / - getAllVisitors")
    class GetAllVisitors {

        @Test
        @DisplayName("should return 200 OK with paged visitor list")
        void shouldReturnOkWithPagedVisitors() {
            when(visitorService.getAllVisitors(PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.getAllVisitors(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(visitorService).getAllVisitors(PAGEABLE);
        }

        @Test
        @DisplayName("should return 200 OK with empty page when no visitors exist")
        void shouldReturnOkWithEmptyPage() {
            PagedResponse<VisitorResponse> emptyPage = PagedResponse.<VisitorResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();

            when(visitorService.getAllVisitors(PAGEABLE)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.getAllVisitors(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getContent()).isEmpty();
            assertThat(result.getBody().getData().getTotalElements()).isZero();

            verify(visitorService).getAllVisitors(PAGEABLE);
        }
    }

    // ========================================================================
    // getVisitor
    // ========================================================================

    @Nested
    @DisplayName("GET /{id} - getVisitor")
    class GetVisitor {

        @Test
        @DisplayName("should return 200 OK with visitor data")
        void shouldReturnOkWithVisitor() {
            when(visitorService.getVisitor(VISITOR_ID)).thenReturn(visitorResponse);

            ResponseEntity<ApiResponse<VisitorResponse>> result = visitorController.getVisitor(VISITOR_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(visitorResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(VISITOR_ID);
            assertThat(result.getBody().getData().getName()).isEqualTo("Jane Doe");

            verify(visitorService).getVisitor(VISITOR_ID);
        }
    }

    // ========================================================================
    // updateVisitor
    // ========================================================================

    @Nested
    @DisplayName("PUT /{id} - updateVisitor")
    class UpdateVisitor {

        private UpdateVisitorRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateVisitorRequest.builder()
                    .name("Jane Updated")
                    .phone("+254799999999")
                    .email("jane.updated@example.com")
                    .purpose("Updated purpose")
                    .build();
        }

        @Test
        @DisplayName("should return 200 OK with success message and updated visitor")
        void shouldReturnOkWithUpdatedVisitor() {
            VisitorResponse updatedResponse = VisitorResponse.builder()
                    .id(VISITOR_ID)
                    .name("Jane Updated")
                    .phone("+254799999999")
                    .email("jane.updated@example.com")
                    .purpose("Updated purpose")
                    .build();

            when(visitorService.updateVisitor(VISITOR_ID, updateRequest)).thenReturn(updatedResponse);

            ResponseEntity<ApiResponse<VisitorResponse>> result =
                    visitorController.updateVisitor(VISITOR_ID, updateRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visitor updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(updatedResponse);
            assertThat(result.getBody().getData().getName()).isEqualTo("Jane Updated");

            verify(visitorService).updateVisitor(VISITOR_ID, updateRequest);
        }
    }

    // ========================================================================
    // deleteVisitor
    // ========================================================================

    @Nested
    @DisplayName("DELETE /{id} - deleteVisitor")
    class DeleteVisitor {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldReturnOkWithSuccessMessage() {
            ResponseEntity<ApiResponse<Void>> result = visitorController.deleteVisitor(VISITOR_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visitor deleted successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(visitorService).deleteVisitor(VISITOR_ID);
        }
    }

    // ========================================================================
    // getMyVisitors
    // ========================================================================

    @Nested
    @DisplayName("GET /my-visitors - getMyVisitors")
    class GetMyVisitors {

        @Test
        @DisplayName("should return 200 OK with paged visitor list for current resident")
        void shouldReturnOkWithMyVisitors() {
            when(visitorService.getMyVisitors(PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.getMyVisitors(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(visitorService).getMyVisitors(PAGEABLE);
        }
    }

    // ========================================================================
    // searchVisitors
    // ========================================================================

    @Nested
    @DisplayName("GET /search - searchVisitors")
    class SearchVisitors {

        @Test
        @DisplayName("should return 200 OK with paged search results")
        void shouldReturnOkWithSearchResults() {
            String query = "Jane";
            when(visitorService.searchVisitors(query, PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.searchVisitors(query, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);

            verify(visitorService).searchVisitors(query, PAGEABLE);
        }

        @Test
        @DisplayName("should pass query and pageable to service")
        void shouldPassQueryAndPageableToService() {
            String query = "+254712345678";
            PagedResponse<VisitorResponse> emptyPage = PagedResponse.<VisitorResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();

            when(visitorService.searchVisitors(query, PAGEABLE)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.searchVisitors(query, PAGEABLE);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getContent()).isEmpty();

            verify(visitorService).searchVisitors(query, PAGEABLE);
        }
    }

    // ========================================================================
    // getExpectedVisitors
    // ========================================================================

    @Nested
    @DisplayName("GET /expected - getExpectedVisitors")
    class GetExpectedVisitors {

        @Test
        @DisplayName("should return 200 OK with paged expected visitors")
        void shouldReturnOkWithExpectedVisitors() {
            when(visitorService.getExpectedVisitors(PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VisitorResponse>>> result =
                    visitorController.getExpectedVisitors(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);

            verify(visitorService).getExpectedVisitors(PAGEABLE);
        }
    }

    // ========================================================================
    // getVisitorPasses
    // ========================================================================

    @Nested
    @DisplayName("GET /{id}/passes - getVisitorPasses")
    class GetVisitorPasses {

        @Test
        @DisplayName("should return 200 OK with list of visit passes")
        void shouldReturnOkWithVisitPasses() {
            List<VisitPassResponse> passes = List.of(visitPassResponse);
            when(visitorService.getVisitorPasses(VISITOR_ID)).thenReturn(passes);

            ResponseEntity<ApiResponse<List<VisitPassResponse>>> result =
                    visitorController.getVisitorPasses(VISITOR_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).hasSize(1);
            assertThat(result.getBody().getData().get(0)).isEqualTo(visitPassResponse);
            assertThat(result.getBody().getData().get(0).getId()).isEqualTo(PASS_ID);

            verify(visitorService).getVisitorPasses(VISITOR_ID);
        }

        @Test
        @DisplayName("should return 200 OK with empty list when no passes exist")
        void shouldReturnOkWithEmptyListWhenNoPasses() {
            when(visitorService.getVisitorPasses(VISITOR_ID)).thenReturn(List.of());

            ResponseEntity<ApiResponse<List<VisitPassResponse>>> result =
                    visitorController.getVisitorPasses(VISITOR_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEmpty();

            verify(visitorService).getVisitorPasses(VISITOR_ID);
        }
    }

    // ========================================================================
    // regeneratePass
    // ========================================================================

    @Nested
    @DisplayName("POST /{id}/passes/regenerate - regeneratePass")
    class RegeneratePass {

        @Test
        @DisplayName("should return 201 CREATED with success message and new pass")
        void shouldReturnCreatedWithNewPass() {
            when(visitorService.regeneratePass(VISITOR_ID)).thenReturn(visitPassResponse);

            ResponseEntity<ApiResponse<VisitPassResponse>> result =
                    visitorController.regeneratePass(VISITOR_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visit pass regenerated successfully");
            assertThat(result.getBody().getData()).isEqualTo(visitPassResponse);

            verify(visitorService).regeneratePass(VISITOR_ID);
        }
    }

    // ========================================================================
    // revokePass
    // ========================================================================

    @Nested
    @DisplayName("POST /{id}/passes/{passId}/revoke - revokePass")
    class RevokePass {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldReturnOkWithSuccessMessage() {
            ResponseEntity<ApiResponse<Void>> result =
                    visitorController.revokePass(VISITOR_ID, PASS_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visit pass revoked successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(visitorService).revokePass(VISITOR_ID, PASS_ID);
        }
    }

    // ========================================================================
    // checkIn
    // ========================================================================

    @Nested
    @DisplayName("POST /check-in - checkIn")
    class CheckIn {

        private VisitorCheckInRequest checkInRequest;
        private VisitorCheckInResponse checkInResponse;

        @BeforeEach
        void setUp() {
            checkInRequest = new VisitorCheckInRequest();
            checkInRequest.setToken("jwt-token");
            checkInRequest.setNote("Arrived at main gate");

            checkInResponse = VisitorCheckInResponse.builder()
                    .visitorId(VISITOR_ID)
                    .visitorName("Jane Doe")
                    .visitorPhone("+254712345678")
                    .visitorType(VisitorType.PEDESTRIAN)
                    .vehiclePlateNumber("KAA123A")
                    .purpose("Delivery")
                    .hostResidentId(INVITED_BY_ID)
                    .hostName("John Resident")
                    .passType(VisitPassType.SINGLE_USE)
                    .usedEntries(1)
                    .maxEntries(1)
                    .validTo(Instant.now().plusSeconds(86400))
                    .checkInTime(Instant.now())
                    .build();
        }

        @Test
        @DisplayName("should return 201 CREATED with success message and check-in response")
        void shouldReturnCreatedWithCheckInResponse() {
            when(visitorService.checkIn(checkInRequest)).thenReturn(checkInResponse);

            ResponseEntity<ApiResponse<VisitorCheckInResponse>> result =
                    visitorController.checkIn(checkInRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visitor checked in successfully");
            assertThat(result.getBody().getData()).isEqualTo(checkInResponse);
            assertThat(result.getBody().getData().getVisitorId()).isEqualTo(VISITOR_ID);
            assertThat(result.getBody().getData().getVisitorName()).isEqualTo("Jane Doe");

            verify(visitorService).checkIn(checkInRequest);
        }

        @Test
        @DisplayName("should delegate to visitorService.checkIn with request")
        void shouldDelegateToService() {
            when(visitorService.checkIn(checkInRequest)).thenReturn(checkInResponse);

            visitorController.checkIn(checkInRequest);

            verify(visitorService).checkIn(checkInRequest);
        }
    }

    // ========================================================================
    // checkOut
    // ========================================================================

    @Nested
    @DisplayName("POST /check-out - checkOut")
    class CheckOut {

        private VisitorCheckOutRequest checkOutRequest;

        @BeforeEach
        void setUp() {
            checkOutRequest = new VisitorCheckOutRequest();
            checkOutRequest.setVisitorId(VISITOR_ID);
            checkOutRequest.setNote("Left via main gate");
        }

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldReturnOkWithSuccessMessage() {
            ResponseEntity<ApiResponse<Void>> result = visitorController.checkOut(checkOutRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Visitor checked out successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(visitorService).checkOut(checkOutRequest);
        }

        @Test
        @DisplayName("should delegate to visitorService.checkOut with request")
        void shouldDelegateToService() {
            visitorController.checkOut(checkOutRequest);

            verify(visitorService).checkOut(checkOutRequest);
        }
    }
}
