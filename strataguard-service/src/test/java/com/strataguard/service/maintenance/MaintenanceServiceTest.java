package com.strataguard.service.maintenance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.maintenance.*;
import com.strataguard.core.entity.MaintenanceComment;
import com.strataguard.core.entity.MaintenanceRequest;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.Unit;
import com.strataguard.core.enums.MaintenanceCategory;
import com.strataguard.core.enums.MaintenancePriority;
import com.strataguard.core.enums.MaintenanceStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.MaintenanceCommentMapper;
import com.strataguard.core.util.MaintenanceMapper;
import com.strataguard.infrastructure.repository.MaintenanceCommentRepository;
import com.strataguard.infrastructure.repository.MaintenanceRequestRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.UnitRepository;
import com.strataguard.service.notification.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
class MaintenanceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID COMMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock
    private MaintenanceRequestRepository requestRepository;

    @Mock
    private MaintenanceCommentRepository commentRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MaintenanceMapper maintenanceMapper;

    @Mock
    private MaintenanceCommentMapper commentMapper;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(maintenanceService, "slaUrgentHours", 4);
        ReflectionTestUtils.setField(maintenanceService, "slaHighHours", 24);
        ReflectionTestUtils.setField(maintenanceService, "slaMediumHours", 72);
        ReflectionTestUtils.setField(maintenanceService, "slaLowHours", 168);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private MaintenanceRequest buildMaintenanceRequest() {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setId(REQUEST_ID);
        request.setTenantId(TENANT_ID);
        request.setRequestNumber("MR-202602-000001");
        request.setUnitId(UNIT_ID);
        request.setEstateId(ESTATE_ID);
        request.setResidentId(RESIDENT_ID);
        request.setTitle("Leaking Pipe");
        request.setDescription("Pipe is leaking in the kitchen");
        request.setCategory(MaintenanceCategory.PLUMBING);
        request.setPriority(MaintenancePriority.HIGH);
        request.setStatus(MaintenanceStatus.OPEN);
        request.setSlaDeadline(Instant.now().plusSeconds(86400));
        return request;
    }

    private MaintenanceResponse buildMaintenanceResponse() {
        return MaintenanceResponse.builder()
                .id(REQUEST_ID)
                .requestNumber("MR-202602-000001")
                .unitId(UNIT_ID)
                .unitNumber("A-101")
                .estateId(ESTATE_ID)
                .residentId(RESIDENT_ID)
                .residentName("John Doe")
                .title("Leaking Pipe")
                .description("Pipe is leaking in the kitchen")
                .category(MaintenanceCategory.PLUMBING)
                .priority(MaintenancePriority.HIGH)
                .status(MaintenanceStatus.OPEN)
                .slaDeadline(Instant.now().plusSeconds(86400))
                .build();
    }

    private Unit buildUnit() {
        Unit unit = new Unit();
        unit.setId(UNIT_ID);
        unit.setTenantId(TENANT_ID);
        unit.setUnitNumber("A-101");
        unit.setEstateId(ESTATE_ID);
        return unit;
    }

    private Resident buildResident() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        return resident;
    }

    private MaintenanceComment buildComment(boolean internal) {
        MaintenanceComment comment = new MaintenanceComment();
        comment.setId(COMMENT_ID);
        comment.setTenantId(TENANT_ID);
        comment.setRequestId(REQUEST_ID);
        comment.setAuthorId(AUTHOR_ID);
        comment.setAuthorName("Admin User");
        comment.setAuthorRole("ADMIN");
        comment.setContent("We will look into this.");
        comment.setInternal(internal);
        return comment;
    }

    private CommentResponse buildCommentResponse(boolean internal) {
        return CommentResponse.builder()
                .id(COMMENT_ID)
                .requestId(REQUEST_ID)
                .authorId(AUTHOR_ID)
                .authorName("Admin User")
                .authorRole("ADMIN")
                .content("We will look into this.")
                .internal(internal)
                .createdAt(Instant.now())
                .build();
    }

    private void stubEnrichResponse(MaintenanceRequest entity) {
        MaintenanceResponse response = buildMaintenanceResponse();
        response.setStatus(entity.getStatus());
        when(maintenanceMapper.toResponse(entity)).thenReturn(response);
        when(unitRepository.findByIdAndTenantId(entity.getUnitId(), TENANT_ID))
                .thenReturn(Optional.of(buildUnit()));
        when(residentRepository.findByIdAndTenantId(entity.getResidentId(), TENANT_ID))
                .thenReturn(Optional.of(buildResident()));
    }

    // ========================================================================
    // createRequest
    // ========================================================================

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("should create request with SLA deadline and OPEN status and send notification")
        void shouldCreateRequestSuccessfully() {
            CreateMaintenanceRequest createReq = CreateMaintenanceRequest.builder()
                    .unitId(UNIT_ID)
                    .estateId(ESTATE_ID)
                    .title("Leaking Pipe")
                    .description("Pipe is leaking in the kitchen")
                    .category(MaintenanceCategory.PLUMBING)
                    .priority(MaintenancePriority.HIGH)
                    .photoUrls("[\"photo1.jpg\"]")
                    .build();

            MaintenanceRequest saved = buildMaintenanceRequest();
            when(requestRepository.countByRequestNumberPrefix(eq(TENANT_ID), any())).thenReturn(0L);
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(saved);
            stubEnrichResponse(saved);

            MaintenanceResponse result = maintenanceService.createRequest(RESIDENT_ID, createReq);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(MaintenanceStatus.OPEN);
            assertThat(result.getResidentName()).isEqualTo("John Doe");
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getSlaDeadline()).isNotNull();

            verify(requestRepository).save(any(MaintenanceRequest.class));
            verify(notificationService).send(any());
        }
    }

    // ========================================================================
    // updateRequest
    // ========================================================================

    @Nested
    @DisplayName("updateRequest")
    class UpdateRequest {

        @Test
        @DisplayName("should update request successfully")
        void shouldUpdateRequestSuccessfully() {
            UpdateMaintenanceRequest updateReq = UpdateMaintenanceRequest.builder()
                    .title("Updated Title")
                    .description("Updated description")
                    .priority(MaintenancePriority.URGENT)
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            MaintenanceRequest savedRequest = buildMaintenanceRequest();
            savedRequest.setTitle("Updated Title");
            savedRequest.setDescription("Updated description");
            savedRequest.setPriority(MaintenancePriority.URGENT);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(savedRequest);
            stubEnrichResponse(savedRequest);

            MaintenanceResponse result = maintenanceService.updateRequest(REQUEST_ID, updateReq);

            assertThat(result).isNotNull();
            assertThat(existing.getTitle()).isEqualTo("Updated Title");
            assertThat(existing.getPriority()).isEqualTo(MaintenancePriority.URGENT);
            verify(requestRepository).save(any(MaintenanceRequest.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when request not found")
        void shouldThrowWhenNotFound() {
            UpdateMaintenanceRequest updateReq = UpdateMaintenanceRequest.builder()
                    .title("Updated Title")
                    .build();

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maintenanceService.updateRequest(REQUEST_ID, updateReq))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MaintenanceRequest");

            verify(requestRepository, never()).save(any());
        }
    }

    // ========================================================================
    // getRequest
    // ========================================================================

    @Nested
    @DisplayName("getRequest")
    class GetRequest {

        @Test
        @DisplayName("should return request when found")
        void shouldReturnRequestWhenFound() {
            MaintenanceRequest entity = buildMaintenanceRequest();

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(entity));
            stubEnrichResponse(entity);

            MaintenanceResponse result = maintenanceService.getRequest(REQUEST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(REQUEST_ID);
            assertThat(result.getRequestNumber()).isEqualTo("MR-202602-000001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> maintenanceService.getRequest(REQUEST_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("MaintenanceRequest");
        }
    }

    // ========================================================================
    // getAllRequests
    // ========================================================================

    @Nested
    @DisplayName("getAllRequests")
    class GetAllRequests {

        @Test
        @DisplayName("should return paged response")
        void shouldReturnPagedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            MaintenanceRequest entity = buildMaintenanceRequest();
            Page<MaintenanceRequest> page = new PageImpl<>(List.of(entity), pageable, 1);

            when(requestRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            stubEnrichResponse(entity);

            PagedResponse<MaintenanceResponse> result = maintenanceService.getAllRequests(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    // ========================================================================
    // assignRequest
    // ========================================================================

    @Nested
    @DisplayName("assignRequest")
    class AssignRequest {

        @Test
        @DisplayName("should assign request and set status to ASSIGNED")
        void shouldAssignRequestSuccessfully() {
            AssignMaintenanceRequest assignReq = AssignMaintenanceRequest.builder()
                    .assignedTo("Mike Plumber")
                    .assignedToPhone("+254700000001")
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.assignRequest(REQUEST_ID, assignReq);

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.ASSIGNED);
            assertThat(existing.getAssignedTo()).isEqualTo("Mike Plumber");
            assertThat(existing.getAssignedToPhone()).isEqualTo("+254700000001");
            assertThat(existing.getAssignedAt()).isNotNull();
            verify(requestRepository).save(existing);
            verify(notificationService).send(any());
        }
    }

    // ========================================================================
    // submitCostEstimate
    // ========================================================================

    @Nested
    @DisplayName("submitCostEstimate")
    class SubmitCostEstimate {

        @Test
        @DisplayName("should submit cost estimate and set status to COST_ESTIMATE")
        void shouldSubmitCostEstimateSuccessfully() {
            CostEstimateRequest costReq = CostEstimateRequest.builder()
                    .estimatedCost(new BigDecimal("5000.00"))
                    .notes("Parts and labor included")
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.ASSIGNED);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.submitCostEstimate(REQUEST_ID, costReq);

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.COST_ESTIMATE);
            assertThat(existing.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(existing.getResolutionNotes()).isEqualTo("Parts and labor included");
            verify(requestRepository).save(existing);
            verify(notificationService).send(any());
        }
    }

    // ========================================================================
    // approveCostEstimate
    // ========================================================================

    @Nested
    @DisplayName("approveCostEstimate")
    class ApproveCostEstimate {

        @Test
        @DisplayName("should approve cost estimate and set status to APPROVED")
        void shouldApproveCostEstimateSuccessfully() {
            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.COST_ESTIMATE);
            existing.setEstimatedCost(new BigDecimal("5000.00"));

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.approveCostEstimate(REQUEST_ID, "Admin User");

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.APPROVED);
            assertThat(existing.getCostApprovedBy()).isEqualTo("Admin User");
            assertThat(existing.getCostApprovedAt()).isNotNull();
            verify(requestRepository).save(existing);
            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when status is not COST_ESTIMATE")
        void shouldThrowWhenStatusIsNotCostEstimate() {
            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.OPEN);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> maintenanceService.approveCostEstimate(REQUEST_ID, "Admin User"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COST_ESTIMATE");

            verify(requestRepository, never()).save(any());
        }
    }

    // ========================================================================
    // resolveRequest
    // ========================================================================

    @Nested
    @DisplayName("resolveRequest")
    class ResolveRequest {

        @Test
        @DisplayName("should resolve request with status RESOLVED and resolvedAt set")
        void shouldResolveRequestSuccessfully() {
            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.IN_PROGRESS);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.resolveRequest(
                    REQUEST_ID, "Fixed the leaking pipe", new BigDecimal("4500.00"));

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.RESOLVED);
            assertThat(existing.getResolvedAt()).isNotNull();
            assertThat(existing.getResolutionNotes()).isEqualTo("Fixed the leaking pipe");
            assertThat(existing.getActualCost()).isEqualByComparingTo(new BigDecimal("4500.00"));
            verify(requestRepository).save(existing);
            verify(notificationService).send(any());
        }
    }

    // ========================================================================
    // closeRequest
    // ========================================================================

    @Nested
    @DisplayName("closeRequest")
    class CloseRequest {

        @Test
        @DisplayName("should close request with status CLOSED and closedAt set")
        void shouldCloseRequestSuccessfully() {
            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.RESOLVED);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.closeRequest(REQUEST_ID);

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.CLOSED);
            assertThat(existing.getClosedAt()).isNotNull();
            verify(requestRepository).save(existing);
        }
    }

    // ========================================================================
    // cancelRequest
    // ========================================================================

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("should cancel request with status CANCELLED")
        void shouldCancelRequestSuccessfully() {
            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.OPEN);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.cancelRequest(REQUEST_ID);

            assertThat(result).isNotNull();
            assertThat(existing.getStatus()).isEqualTo(MaintenanceStatus.CANCELLED);
            verify(requestRepository).save(existing);
        }
    }

    // ========================================================================
    // rateRequest
    // ========================================================================

    @Nested
    @DisplayName("rateRequest")
    class RateRequest {

        @Test
        @DisplayName("should rate request successfully when resident matches and status is RESOLVED")
        void shouldRateRequestSuccessfully() {
            RateMaintenanceRequest rateReq = RateMaintenanceRequest.builder()
                    .rating(5)
                    .comment("Excellent service!")
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.RESOLVED);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(requestRepository.save(any(MaintenanceRequest.class))).thenReturn(existing);
            stubEnrichResponse(existing);

            MaintenanceResponse result = maintenanceService.rateRequest(REQUEST_ID, RESIDENT_ID, rateReq);

            assertThat(result).isNotNull();
            assertThat(existing.getSatisfactionRating()).isEqualTo(5);
            assertThat(existing.getSatisfactionComment()).isEqualTo("Excellent service!");
            verify(requestRepository).save(existing);
        }

        @Test
        @DisplayName("should throw IllegalStateException when resident does not match")
        void shouldThrowWhenResidentDoesNotMatch() {
            RateMaintenanceRequest rateReq = RateMaintenanceRequest.builder()
                    .rating(4)
                    .comment("Good")
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.RESOLVED);
            UUID otherResidentId = UUID.fromString("00000000-0000-0000-0000-000000000099");

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> maintenanceService.rateRequest(REQUEST_ID, otherResidentId, rateReq))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only the requesting resident");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when status is not RESOLVED or CLOSED")
        void shouldThrowWhenStatusIsNotResolved() {
            RateMaintenanceRequest rateReq = RateMaintenanceRequest.builder()
                    .rating(3)
                    .comment("Okay")
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            existing.setStatus(MaintenanceStatus.OPEN);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> maintenanceService.rateRequest(REQUEST_ID, RESIDENT_ID, rateReq))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("resolved or closed");

            verify(requestRepository, never()).save(any());
        }
    }

    // ========================================================================
    // addComment
    // ========================================================================

    @Nested
    @DisplayName("addComment")
    class AddComment {

        @Test
        @DisplayName("should add public comment and send notification")
        void shouldAddPublicCommentSuccessfully() {
            AddCommentRequest commentReq = AddCommentRequest.builder()
                    .content("We will look into this.")
                    .internal(false)
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            MaintenanceComment savedComment = buildComment(false);
            CommentResponse expectedResponse = buildCommentResponse(false);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(commentRepository.save(any(MaintenanceComment.class))).thenReturn(savedComment);
            when(commentMapper.toResponse(savedComment)).thenReturn(expectedResponse);

            CommentResponse result = maintenanceService.addComment(
                    REQUEST_ID, AUTHOR_ID, "Admin User", "ADMIN", commentReq);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("We will look into this.");
            assertThat(result.isInternal()).isFalse();
            verify(commentRepository).save(any(MaintenanceComment.class));
            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should add internal comment without sending notification")
        void shouldAddInternalCommentWithoutNotification() {
            AddCommentRequest commentReq = AddCommentRequest.builder()
                    .content("Internal note: check plumbing first")
                    .internal(true)
                    .build();

            MaintenanceRequest existing = buildMaintenanceRequest();
            MaintenanceComment savedComment = buildComment(true);
            CommentResponse expectedResponse = buildCommentResponse(true);

            when(requestRepository.findByIdAndTenantId(REQUEST_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(commentRepository.save(any(MaintenanceComment.class))).thenReturn(savedComment);
            when(commentMapper.toResponse(savedComment)).thenReturn(expectedResponse);

            CommentResponse result = maintenanceService.addComment(
                    REQUEST_ID, AUTHOR_ID, "Admin User", "ADMIN", commentReq);

            assertThat(result).isNotNull();
            assertThat(result.isInternal()).isTrue();
            verify(commentRepository).save(any(MaintenanceComment.class));
            verify(notificationService, never()).send(any());
        }
    }

    // ========================================================================
    // getComments
    // ========================================================================

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("should include internal comments when includeInternal is true")
        void shouldIncludeInternalComments() {
            Pageable pageable = PageRequest.of(0, 10);
            MaintenanceComment publicComment = buildComment(false);
            MaintenanceComment internalComment = buildComment(true);
            internalComment.setId(UUID.randomUUID());
            Page<MaintenanceComment> page = new PageImpl<>(List.of(publicComment, internalComment), pageable, 2);

            when(commentRepository.findByRequestIdAndTenantId(REQUEST_ID, TENANT_ID, pageable)).thenReturn(page);
            when(commentMapper.toResponse(publicComment)).thenReturn(buildCommentResponse(false));
            when(commentMapper.toResponse(internalComment)).thenReturn(buildCommentResponse(true));

            PagedResponse<CommentResponse> result = maintenanceService.getComments(REQUEST_ID, true, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(commentRepository).findByRequestIdAndTenantId(REQUEST_ID, TENANT_ID, pageable);
            verify(commentRepository, never()).findPublicByRequestIdAndTenantId(any(), any(), any());
        }

        @Test
        @DisplayName("should exclude internal comments when includeInternal is false")
        void shouldExcludeInternalComments() {
            Pageable pageable = PageRequest.of(0, 10);
            MaintenanceComment publicComment = buildComment(false);
            Page<MaintenanceComment> page = new PageImpl<>(List.of(publicComment), pageable, 1);

            when(commentRepository.findPublicByRequestIdAndTenantId(REQUEST_ID, TENANT_ID, pageable)).thenReturn(page);
            when(commentMapper.toResponse(publicComment)).thenReturn(buildCommentResponse(false));

            PagedResponse<CommentResponse> result = maintenanceService.getComments(REQUEST_ID, false, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(commentRepository).findPublicByRequestIdAndTenantId(REQUEST_ID, TENANT_ID, pageable);
            verify(commentRepository, never()).findByRequestIdAndTenantId(any(), any(), any());
        }
    }

    // ========================================================================
    // getDashboard
    // ========================================================================

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard with all aggregated values")
        void shouldReturnDashboard() {
            when(requestRepository.countByTenantId(TENANT_ID)).thenReturn(50L);
            when(requestRepository.countByStatusAndTenantId(MaintenanceStatus.OPEN, TENANT_ID)).thenReturn(10L);
            when(requestRepository.countByStatusAndTenantId(MaintenanceStatus.ASSIGNED, TENANT_ID)).thenReturn(8L);
            when(requestRepository.countByStatusAndTenantId(MaintenanceStatus.IN_PROGRESS, TENANT_ID)).thenReturn(5L);
            when(requestRepository.countByStatusAndTenantId(MaintenanceStatus.RESOLVED, TENANT_ID)).thenReturn(20L);
            when(requestRepository.countSlaBreachedByTenantId(TENANT_ID)).thenReturn(3L);
            when(requestRepository.averageSatisfactionRatingByTenantId(TENANT_ID)).thenReturn(4.2);

            MaintenanceDashboardResponse result = maintenanceService.getDashboard();

            assertThat(result).isNotNull();
            assertThat(result.getTotalRequests()).isEqualTo(50L);
            assertThat(result.getOpenRequests()).isEqualTo(10L);
            assertThat(result.getAssignedRequests()).isEqualTo(8L);
            assertThat(result.getInProgressRequests()).isEqualTo(5L);
            assertThat(result.getResolvedRequests()).isEqualTo(20L);
            assertThat(result.getSlaBreachedRequests()).isEqualTo(3L);
            assertThat(result.getAverageSatisfactionRating()).isEqualTo(4.2);
        }

        @Test
        @DisplayName("should default average rating to 0.0 when null")
        void shouldDefaultAverageRatingToZeroWhenNull() {
            when(requestRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
            when(requestRepository.countByStatusAndTenantId(any(), eq(TENANT_ID))).thenReturn(0L);
            when(requestRepository.countSlaBreachedByTenantId(TENANT_ID)).thenReturn(0L);
            when(requestRepository.averageSatisfactionRatingByTenantId(TENANT_ID)).thenReturn(null);

            MaintenanceDashboardResponse result = maintenanceService.getDashboard();

            assertThat(result).isNotNull();
            assertThat(result.getAverageSatisfactionRating()).isEqualTo(0.0);
        }
    }
}
