package com.strataguard.service.visitor;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.visitor.*;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.VisitPass;
import com.strataguard.core.entity.Visitor;
import com.strataguard.core.enums.VisitPassStatus;
import com.strataguard.core.enums.VisitPassType;
import com.strataguard.core.enums.VisitorStatus;
import com.strataguard.core.enums.VisitorType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.VisitPassMapper;
import com.strataguard.core.util.VisitorMapper;
import com.strataguard.infrastructure.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorServiceTest {

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private VisitPassRepository visitPassRepository;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private GateAccessLogRepository gateAccessLogRepository;

    @Mock
    private VisitorMapper visitorMapper;

    @Mock
    private VisitPassMapper visitPassMapper;

    @Mock
    private VisitorPassService visitorPassService;

    @InjectMocks
    private VisitorService visitorService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VISITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID PASS_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final String CURRENT_USER_ID = "user-keycloak-id-001";

    private Visitor visitor;
    private VisitorResponse visitorResponse;
    private Resident resident;
    private VisitPass visitPass;
    private VisitPassResponse visitPassResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setUserId(CURRENT_USER_ID);
        resident.setFirstName("Alice");
        resident.setLastName("Smith");
        resident.setPhone("+254700000000");
        resident.setEmail("alice@example.com");

        visitor = new Visitor();
        visitor.setId(VISITOR_ID);
        visitor.setTenantId(TENANT_ID);
        visitor.setName("Bob Builder");
        visitor.setPhone("+254711111111");
        visitor.setEmail("bob@example.com");
        visitor.setPurpose("Delivery");
        visitor.setInvitedBy(RESIDENT_ID);
        visitor.setVisitorType(VisitorType.PEDESTRIAN);
        visitor.setVehiclePlateNumber(null);
        visitor.setStatus(VisitorStatus.PENDING);
        visitor.setDeleted(false);
        visitor.setCreatedAt(Instant.now());
        visitor.setUpdatedAt(Instant.now());

        visitorResponse = VisitorResponse.builder()
                .id(VISITOR_ID)
                .name("Bob Builder")
                .phone("+254711111111")
                .email("bob@example.com")
                .purpose("Delivery")
                .invitedBy(RESIDENT_ID)
                .invitedByName("Alice Smith")
                .visitorType(VisitorType.PEDESTRIAN)
                .status(VisitorStatus.PENDING)
                .createdAt(visitor.getCreatedAt())
                .updatedAt(visitor.getUpdatedAt())
                .build();

        visitPass = new VisitPass();
        visitPass.setId(PASS_ID);
        visitPass.setTenantId(TENANT_ID);
        visitPass.setVisitorId(VISITOR_ID);
        visitPass.setPassCode("pass-code-001");
        visitPass.setQrData("qr-data-base64");
        visitPass.setToken("token-data");
        visitPass.setPassType(VisitPassType.SINGLE_USE);
        visitPass.setValidFrom(Instant.now());
        visitPass.setValidTo(Instant.now().plus(24, ChronoUnit.HOURS));
        visitPass.setMaxEntries(1);
        visitPass.setUsedEntries(0);
        visitPass.setStatus(VisitPassStatus.ACTIVE);
        visitPass.setVerificationCode("123456");
        visitPass.setDeleted(false);
        visitPass.setCreatedAt(Instant.now());

        visitPassResponse = VisitPassResponse.builder()
                .id(PASS_ID)
                .visitorId(VISITOR_ID)
                .passCode("pass-code-001")
                .qrData("qr-data-base64")
                .token("token-data")
                .verificationCode("123456")
                .passType(VisitPassType.SINGLE_USE)
                .validFrom(visitPass.getValidFrom())
                .validTo(visitPass.getValidTo())
                .maxEntries(1)
                .usedEntries(0)
                .status(VisitPassStatus.ACTIVE)
                .createdAt(visitPass.getCreatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(CURRENT_USER_ID, null, Collections.emptyList());
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ========================================================================
    // createVisitor
    // ========================================================================

    @Nested
    @DisplayName("createVisitor")
    class CreateVisitor {

        private CreateVisitorRequest createRequest;

        @BeforeEach
        void setUp() {
            mockSecurityContext();

            createRequest = CreateVisitorRequest.builder()
                    .name("Bob Builder")
                    .phone("+254711111111")
                    .email("bob@example.com")
                    .purpose("Delivery")
                    .visitorType(VisitorType.PEDESTRIAN)
                    .passType(VisitPassType.SINGLE_USE)
                    .validFrom(Instant.now())
                    .validTo(Instant.now().plus(24, ChronoUnit.HOURS))
                    .maxEntries(1)
                    .build();
        }

        @Test
        @DisplayName("should create visitor successfully and generate a pass")
        void shouldCreateVisitorSuccessfully() {
            when(residentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(visitorMapper.toEntity(createRequest)).thenReturn(visitor);
            when(visitorRepository.save(visitor)).thenReturn(visitor);
            when(visitorPassService.generateToken(any(String.class), eq(VISITOR_ID), eq(TENANT_ID), any(Long.class)))
                    .thenReturn("generated-token");
            when(visitPassRepository.save(any(VisitPass.class))).thenReturn(visitPass);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);

            VisitorResponse result = visitorService.createVisitor(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VISITOR_ID);
            assertThat(result.getName()).isEqualTo("Bob Builder");
            assertThat(result.getPhone()).isEqualTo("+254711111111");
            assertThat(result.getPurpose()).isEqualTo("Delivery");
            assertThat(result.getVisitorType()).isEqualTo(VisitorType.PEDESTRIAN);
            assertThat(result.getInvitedByName()).isEqualTo("Alice Smith");

            verify(residentRepository).findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID);
            verify(visitorMapper).toEntity(createRequest);
            verify(visitorRepository).save(visitor);
            verify(visitPassRepository).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should set tenantId and invitedBy from context and resident lookup")
        void shouldSetTenantIdAndInvitedBy() {
            when(residentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(visitorMapper.toEntity(createRequest)).thenReturn(visitor);
            when(visitorRepository.save(visitor)).thenReturn(visitor);
            when(visitorPassService.generateToken(any(String.class), eq(VISITOR_ID), eq(TENANT_ID), any(Long.class)))
                    .thenReturn("generated-token");
            when(visitPassRepository.save(any(VisitPass.class))).thenReturn(visitPass);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);

            visitorService.createVisitor(createRequest);

            assertThat(visitor.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(visitor.getInvitedBy()).isEqualTo(RESIDENT_ID);
        }

        @Test
        @DisplayName("should normalize vehicle plate number when provided")
        void shouldNormalizePlateNumber() {
            CreateVisitorRequest vehicleRequest = CreateVisitorRequest.builder()
                    .name("Car Visitor")
                    .visitorType(VisitorType.VEHICLE)
                    .vehiclePlateNumber("KAA 123A")
                    .passType(VisitPassType.SINGLE_USE)
                    .validFrom(Instant.now())
                    .validTo(Instant.now().plus(24, ChronoUnit.HOURS))
                    .maxEntries(1)
                    .build();

            Visitor vehicleVisitor = new Visitor();
            vehicleVisitor.setId(VISITOR_ID);
            vehicleVisitor.setTenantId(TENANT_ID);
            vehicleVisitor.setName("Car Visitor");
            vehicleVisitor.setVisitorType(VisitorType.VEHICLE);

            when(residentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(visitorMapper.toEntity(vehicleRequest)).thenReturn(vehicleVisitor);
            when(visitorRepository.save(vehicleVisitor)).thenReturn(vehicleVisitor);
            when(visitorPassService.generateToken(any(String.class), eq(VISITOR_ID), eq(TENANT_ID), any(Long.class)))
                    .thenReturn("generated-token");
            when(visitPassRepository.save(any(VisitPass.class))).thenReturn(visitPass);
            when(visitorMapper.toResponse(vehicleVisitor)).thenReturn(visitorResponse);

            visitorService.createVisitor(vehicleRequest);

            assertThat(vehicleVisitor.getVehiclePlateNumber()).isEqualTo("KAA123A");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when current user is not a resident")
        void shouldThrowWhenCurrentUserNotResident() {
            when(residentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.createVisitor(createRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("userId");

            verify(visitorRepository, never()).save(any(Visitor.class));
            verify(visitPassRepository, never()).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should set invitedByName on response from resident first and last name")
        void shouldSetInvitedByNameOnResponse() {
            when(residentRepository.findByUserIdAndTenantId(CURRENT_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(visitorMapper.toEntity(createRequest)).thenReturn(visitor);
            when(visitorRepository.save(visitor)).thenReturn(visitor);
            when(visitorPassService.generateToken(any(String.class), eq(VISITOR_ID), eq(TENANT_ID), any(Long.class)))
                    .thenReturn("generated-token");
            when(visitPassRepository.save(any(VisitPass.class))).thenReturn(visitPass);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);

            VisitorResponse result = visitorService.createVisitor(createRequest);

            assertThat(result.getInvitedByName()).isEqualTo("Alice Smith");
        }
    }

    // ========================================================================
    // getVisitor
    // ========================================================================

    @Nested
    @DisplayName("getVisitor")
    class GetVisitor {

        @Test
        @DisplayName("should return visitor when found by id and tenant")
        void shouldReturnVisitorWhenFound() {
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            VisitorResponse result = visitorService.getVisitor(VISITOR_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VISITOR_ID);
            assertThat(result.getName()).isEqualTo("Bob Builder");
            assertThat(result.getPhone()).isEqualTo("+254711111111");
            assertThat(result.getInvitedByName()).isEqualTo("Alice Smith");

            verify(visitorRepository).findByIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitorMapper).toResponse(visitor);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when visitor not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.getVisitor(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Visitor")
                    .hasMessageContaining("id");

            verify(visitorRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(visitorMapper, never()).toResponse(any(Visitor.class));
        }
    }

    // ========================================================================
    // getAllVisitors
    // ========================================================================

    @Nested
    @DisplayName("getAllVisitors")
    class GetAllVisitors {

        @Test
        @DisplayName("should return paginated list of visitors for tenant")
        void shouldReturnPaginatedVisitors() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Visitor> page = new PageImpl<>(List.of(visitor), pageable, 1);

            when(visitorRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            PagedResponse<VisitorResponse> result = visitorService.getAllVisitors(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Bob Builder");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(visitorRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no visitors exist for tenant")
        void shouldReturnEmptyPageWhenNoVisitors() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Visitor> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(visitorRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<VisitorResponse> result = visitorService.getAllVisitors(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            Page<Visitor> page = new PageImpl<>(List.of(), pageable, 0);

            when(visitorRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

            visitorService.getAllVisitors(pageable);

            verify(visitorRepository).findAllByTenantId(TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // updateVisitor
    // ========================================================================

    @Nested
    @DisplayName("updateVisitor")
    class UpdateVisitor {

        private UpdateVisitorRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateVisitorRequest.builder()
                    .name("Bob Builder Updated")
                    .phone("+254722222222")
                    .email("bob.updated@example.com")
                    .purpose("Meeting")
                    .vehiclePlateNumber("KBB 456B")
                    .build();
        }

        @Test
        @DisplayName("should update visitor successfully when found")
        void shouldUpdateVisitorSuccessfully() {
            Visitor updatedVisitor = new Visitor();
            updatedVisitor.setId(VISITOR_ID);
            updatedVisitor.setTenantId(TENANT_ID);
            updatedVisitor.setName("Bob Builder Updated");
            updatedVisitor.setPhone("+254722222222");
            updatedVisitor.setEmail("bob.updated@example.com");
            updatedVisitor.setPurpose("Meeting");
            updatedVisitor.setInvitedBy(RESIDENT_ID);
            updatedVisitor.setVisitorType(VisitorType.PEDESTRIAN);
            updatedVisitor.setVehiclePlateNumber("KBB456B");
            updatedVisitor.setStatus(VisitorStatus.PENDING);

            VisitorResponse updatedResponse = VisitorResponse.builder()
                    .id(VISITOR_ID)
                    .name("Bob Builder Updated")
                    .phone("+254722222222")
                    .email("bob.updated@example.com")
                    .purpose("Meeting")
                    .invitedBy(RESIDENT_ID)
                    .invitedByName("Alice Smith")
                    .visitorType(VisitorType.PEDESTRIAN)
                    .vehiclePlateNumber("KBB456B")
                    .status(VisitorStatus.PENDING)
                    .build();

            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitorRepository.save(visitor)).thenReturn(updatedVisitor);
            when(visitorMapper.toResponse(updatedVisitor)).thenReturn(updatedResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            VisitorResponse result = visitorService.updateVisitor(VISITOR_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Bob Builder Updated");
            assertThat(result.getPhone()).isEqualTo("+254722222222");
            assertThat(result.getEmail()).isEqualTo("bob.updated@example.com");
            assertThat(result.getPurpose()).isEqualTo("Meeting");
            assertThat(result.getVehiclePlateNumber()).isEqualTo("KBB456B");
            assertThat(result.getInvitedByName()).isEqualTo("Alice Smith");

            verify(visitorRepository).findByIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitorMapper).updateEntity(updateRequest, visitor);
            verify(visitorRepository).save(visitor);
            verify(visitorMapper).toResponse(updatedVisitor);
        }

        @Test
        @DisplayName("should normalize plate number on update")
        void shouldNormalizePlateNumberOnUpdate() {
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitorRepository.save(visitor)).thenReturn(visitor);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            visitorService.updateVisitor(VISITOR_ID, updateRequest);

            // PlateNumberUtils.normalize("KBB 456B") -> "KBB456B"
            assertThat(updateRequest.getVehiclePlateNumber()).isEqualTo("KBB456B");
        }

        @Test
        @DisplayName("should not normalize plate number when null on update")
        void shouldNotNormalizePlateNumberWhenNull() {
            UpdateVisitorRequest noPlateRequest = UpdateVisitorRequest.builder()
                    .name("Updated Name")
                    .purpose("Updated purpose")
                    .build();

            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitorRepository.save(visitor)).thenReturn(visitor);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            visitorService.updateVisitor(VISITOR_ID, noPlateRequest);

            assertThat(noPlateRequest.getVehiclePlateNumber()).isNull();
            verify(visitorMapper).updateEntity(noPlateRequest, visitor);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when visitor not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.updateVisitor(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Visitor")
                    .hasMessageContaining("id");

            verify(visitorRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(visitorMapper, never()).updateEntity(any(), any());
            verify(visitorRepository, never()).save(any(Visitor.class));
        }
    }

    // ========================================================================
    // deleteVisitor
    // ========================================================================

    @Nested
    @DisplayName("deleteVisitor")
    class DeleteVisitor {

        @Test
        @DisplayName("should soft-delete visitor and revoke all active passes")
        void shouldSoftDeleteVisitorAndRevokeActivePasses() {
            VisitPass activePass1 = new VisitPass();
            activePass1.setId(UUID.randomUUID());
            activePass1.setVisitorId(VISITOR_ID);
            activePass1.setStatus(VisitPassStatus.ACTIVE);

            VisitPass activePass2 = new VisitPass();
            activePass2.setId(UUID.randomUUID());
            activePass2.setVisitorId(VISITOR_ID);
            activePass2.setStatus(VisitPassStatus.ACTIVE);

            VisitPass usedPass = new VisitPass();
            usedPass.setId(UUID.randomUUID());
            usedPass.setVisitorId(VISITOR_ID);
            usedPass.setStatus(VisitPassStatus.USED);

            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(List.of(activePass1, activePass2, usedPass));

            visitorService.deleteVisitor(VISITOR_ID);

            assertThat(visitor.isDeleted()).isTrue();
            assertThat(activePass1.getStatus()).isEqualTo(VisitPassStatus.REVOKED);
            assertThat(activePass2.getStatus()).isEqualTo(VisitPassStatus.REVOKED);
            assertThat(usedPass.getStatus()).isEqualTo(VisitPassStatus.USED);

            verify(visitorRepository).findByIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitorRepository).save(visitor);
            verify(visitPassRepository).findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitPassRepository, times(2)).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should soft-delete visitor with no passes without errors")
        void shouldSoftDeleteVisitorWithNoPasses() {
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(List.of());

            visitorService.deleteVisitor(VISITOR_ID);

            assertThat(visitor.isDeleted()).isTrue();

            verify(visitorRepository).save(visitor);
            verify(visitPassRepository, never()).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when visitor not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.deleteVisitor(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Visitor")
                    .hasMessageContaining("id");

            verify(visitorRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(visitorRepository, never()).save(any(Visitor.class));
            verify(visitPassRepository, never()).findByVisitorIdAndTenantId(any(), any());
        }
    }

    // ========================================================================
    // searchVisitors
    // ========================================================================

    @Nested
    @DisplayName("searchVisitors")
    class SearchVisitors {

        @Test
        @DisplayName("should return matching visitors for search query")
        void shouldReturnMatchingVisitors() {
            String query = "Bob";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Visitor> page = new PageImpl<>(List.of(visitor), pageable, 1);

            when(visitorRepository.search(query, TENANT_ID, pageable)).thenReturn(page);
            when(visitorMapper.toResponse(visitor)).thenReturn(visitorResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            PagedResponse<VisitorResponse> result = visitorService.searchVisitors(query, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Bob Builder");
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(visitorRepository).search(query, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no visitors match search")
        void shouldReturnEmptyPageWhenNoMatch() {
            String query = "NonExistent";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Visitor> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(visitorRepository.search(query, TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<VisitorResponse> result = visitorService.searchVisitors(query, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ========================================================================
    // revokePass
    // ========================================================================

    @Nested
    @DisplayName("revokePass")
    class RevokePass {

        @Test
        @DisplayName("should revoke pass successfully when visitor and pass exist and match")
        void shouldRevokePassSuccessfully() {
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByIdAndTenantId(PASS_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitPass));

            visitorService.revokePass(VISITOR_ID, PASS_ID);

            assertThat(visitPass.getStatus()).isEqualTo(VisitPassStatus.REVOKED);

            verify(visitorRepository).findByIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitPassRepository).findByIdAndTenantId(PASS_ID, TENANT_ID);
            verify(visitPassRepository).save(visitPass);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when visitor not found for revokePass")
        void shouldThrowWhenVisitorNotFound() {
            UUID nonExistentVisitorId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(nonExistentVisitorId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.revokePass(nonExistentVisitorId, PASS_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Visitor")
                    .hasMessageContaining("id");

            verify(visitPassRepository, never()).findByIdAndTenantId(any(), any());
            verify(visitPassRepository, never()).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when pass not found for revokePass")
        void shouldThrowWhenPassNotFound() {
            UUID nonExistentPassId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByIdAndTenantId(nonExistentPassId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.revokePass(VISITOR_ID, nonExistentPassId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VisitPass")
                    .hasMessageContaining("id");

            verify(visitPassRepository, never()).save(any(VisitPass.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when pass does not belong to visitor")
        void shouldThrowWhenPassDoesNotBelongToVisitor() {
            UUID otherVisitorId = UUID.randomUUID();

            VisitPass mismatchedPass = new VisitPass();
            mismatchedPass.setId(PASS_ID);
            mismatchedPass.setVisitorId(otherVisitorId);
            mismatchedPass.setStatus(VisitPassStatus.ACTIVE);

            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByIdAndTenantId(PASS_ID, TENANT_ID))
                    .thenReturn(Optional.of(mismatchedPass));

            assertThatThrownBy(() -> visitorService.revokePass(VISITOR_ID, PASS_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VisitPass")
                    .hasMessageContaining("id");

            verify(visitPassRepository, never()).save(any(VisitPass.class));
        }
    }

    // ========================================================================
    // getVisitorPasses
    // ========================================================================

    @Nested
    @DisplayName("getVisitorPasses")
    class GetVisitorPasses {

        @Test
        @DisplayName("should return list of passes for a visitor")
        void shouldReturnPassesForVisitor() {
            VisitPass pass2 = new VisitPass();
            pass2.setId(UUID.randomUUID());
            pass2.setVisitorId(VISITOR_ID);
            pass2.setStatus(VisitPassStatus.USED);

            VisitPassResponse passResponse2 = VisitPassResponse.builder()
                    .id(pass2.getId())
                    .visitorId(VISITOR_ID)
                    .status(VisitPassStatus.USED)
                    .build();

            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(List.of(visitPass, pass2));
            when(visitPassMapper.toResponse(visitPass)).thenReturn(visitPassResponse);
            when(visitPassMapper.toResponse(pass2)).thenReturn(passResponse2);

            List<VisitPassResponse> result = visitorService.getVisitorPasses(VISITOR_ID);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(PASS_ID);
            assertThat(result.get(0).getStatus()).isEqualTo(VisitPassStatus.ACTIVE);
            assertThat(result.get(1).getStatus()).isEqualTo(VisitPassStatus.USED);

            verify(visitorRepository).findByIdAndTenantId(VISITOR_ID, TENANT_ID);
            verify(visitPassRepository).findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when visitor has no passes")
        void shouldReturnEmptyListWhenNoPasses() {
            when(visitorRepository.findByIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(Optional.of(visitor));
            when(visitPassRepository.findByVisitorIdAndTenantId(VISITOR_ID, TENANT_ID))
                    .thenReturn(List.of());

            List<VisitPassResponse> result = visitorService.getVisitorPasses(VISITOR_ID);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when visitor not found for getVisitorPasses")
        void shouldThrowWhenVisitorNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(visitorRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> visitorService.getVisitorPasses(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Visitor")
                    .hasMessageContaining("id");

            verify(visitPassRepository, never()).findByVisitorIdAndTenantId(any(), any());
        }
    }
}
