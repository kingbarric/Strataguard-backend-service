package com.strataguard.service.resident;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.tenancy.CreateTenancyRequest;
import com.strataguard.core.dto.tenancy.TenancyResponse;
import com.strataguard.core.dto.tenancy.UpdateTenancyRequest;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.entity.Unit;
import com.strataguard.core.enums.TenancyStatus;
import com.strataguard.core.enums.TenancyType;
import com.strataguard.core.enums.UnitStatus;
import com.strataguard.core.exception.InvalidStateTransitionException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.TenancyMapper;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.TenancyRepository;
import com.strataguard.infrastructure.repository.UnitRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyServiceTest {

    @Mock
    private TenancyRepository tenancyRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private TenancyMapper tenancyMapper;

    @InjectMocks
    private TenancyService tenancyService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID TENANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

    private Resident resident;
    private Unit unit;
    private Tenancy tenancy;
    private TenancyResponse tenancyResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        resident.setEmail("john.doe@example.com");
        resident.setActive(true);
        resident.setDeleted(false);

        unit = new Unit();
        unit.setId(UNIT_ID);
        unit.setTenantId(TENANT_ID);
        unit.setUnitNumber("A-101");
        unit.setStatus(UnitStatus.VACANT);
        unit.setActive(true);
        unit.setDeleted(false);

        tenancy = new Tenancy();
        tenancy.setId(TENANCY_ID);
        tenancy.setTenantId(TENANT_ID);
        tenancy.setResidentId(RESIDENT_ID);
        tenancy.setUnitId(UNIT_ID);
        tenancy.setTenancyType(TenancyType.OWNER);
        tenancy.setStartDate(LocalDate.of(2025, 1, 1));
        tenancy.setEndDate(null);
        tenancy.setStatus(TenancyStatus.ACTIVE);
        tenancy.setLeaseReference("LEASE-001");
        tenancy.setActive(true);
        tenancy.setDeleted(false);
        tenancy.setCreatedAt(Instant.now());
        tenancy.setUpdatedAt(Instant.now());

        tenancyResponse = TenancyResponse.builder()
                .id(TENANCY_ID)
                .residentId(RESIDENT_ID)
                .unitId(UNIT_ID)
                .tenancyType(TenancyType.OWNER)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(null)
                .status(TenancyStatus.ACTIVE)
                .leaseReference("LEASE-001")
                .active(true)
                .createdAt(tenancy.getCreatedAt())
                .updatedAt(tenancy.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // createTenancy
    // ========================================================================

    @Nested
    @DisplayName("createTenancy")
    class CreateTenancy {

        private CreateTenancyRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateTenancyRequest.builder()
                    .residentId(RESIDENT_ID)
                    .unitId(UNIT_ID)
                    .tenancyType(TenancyType.OWNER)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(null)
                    .leaseReference("LEASE-001")
                    .build();
        }

        @Test
        @DisplayName("should create tenancy successfully and set unit to OCCUPIED")
        void shouldCreateTenancySuccessfully() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.OWNER, TENANT_ID))
                    .thenReturn(false);
            when(tenancyMapper.toEntity(createRequest)).thenReturn(tenancy);
            when(tenancyRepository.save(tenancy)).thenReturn(tenancy);
            when(tenancyMapper.toResponse(tenancy)).thenReturn(tenancyResponse);

            TenancyResponse result = tenancyService.createTenancy(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TENANCY_ID);
            assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getUnitId()).isEqualTo(UNIT_ID);
            assertThat(result.getTenancyType()).isEqualTo(TenancyType.OWNER);
            assertThat(result.getStatus()).isEqualTo(TenancyStatus.ACTIVE);
            assertThat(result.getLeaseReference()).isEqualTo("LEASE-001");
            assertThat(result.isActive()).isTrue();

            // Verify unit was set to OCCUPIED
            assertThat(unit.getStatus()).isEqualTo(UnitStatus.OCCUPIED);

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(tenancyRepository).existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.OWNER, TENANT_ID);
            verify(tenancyMapper).toEntity(createRequest);
            verify(tenancyRepository).save(tenancy);
            verify(unitRepository).save(unit);
            verify(tenancyMapper).toResponse(tenancy);
        }

        @Test
        @DisplayName("should set tenant ID from TenantContext on created tenancy")
        void shouldSetTenantIdFromContext() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.OWNER, TENANT_ID))
                    .thenReturn(false);
            when(tenancyMapper.toEntity(createRequest)).thenReturn(tenancy);
            when(tenancyRepository.save(tenancy)).thenReturn(tenancy);
            when(tenancyMapper.toResponse(tenancy)).thenReturn(tenancyResponse);

            tenancyService.createTenancy(createRequest);

            assertThat(tenancy.getTenantId()).isEqualTo(TENANT_ID);
            verify(tenancyRepository).save(tenancy);
        }

        @Test
        @DisplayName("should skip duplicate check for DEPENDENT tenancy type")
        void shouldSkipDuplicateCheckForDependentType() {
            CreateTenancyRequest dependentRequest = CreateTenancyRequest.builder()
                    .residentId(RESIDENT_ID)
                    .unitId(UNIT_ID)
                    .tenancyType(TenancyType.DEPENDENT)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .build();

            Tenancy dependentTenancy = new Tenancy();
            dependentTenancy.setId(TENANCY_ID);
            dependentTenancy.setTenancyType(TenancyType.DEPENDENT);
            dependentTenancy.setStatus(TenancyStatus.ACTIVE);

            TenancyResponse dependentResponse = TenancyResponse.builder()
                    .id(TENANCY_ID)
                    .tenancyType(TenancyType.DEPENDENT)
                    .status(TenancyStatus.ACTIVE)
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyMapper.toEntity(dependentRequest)).thenReturn(dependentTenancy);
            when(tenancyRepository.save(dependentTenancy)).thenReturn(dependentTenancy);
            when(tenancyMapper.toResponse(dependentTenancy)).thenReturn(dependentResponse);

            TenancyResponse result = tenancyService.createTenancy(dependentRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTenancyType()).isEqualTo(TenancyType.DEPENDENT);

            verify(tenancyRepository, never())
                    .existsActiveByUnitIdAndTenancyTypeAndTenantId(any(), any(), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident does not exist")
        void shouldThrowResourceNotFoundExceptionWhenResidentNotFound() {
            UUID nonExistentResidentId = UUID.randomUUID();
            CreateTenancyRequest requestWithBadResident = CreateTenancyRequest.builder()
                    .residentId(nonExistentResidentId)
                    .unitId(UNIT_ID)
                    .tenancyType(TenancyType.OWNER)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(residentRepository.findByIdAndTenantId(nonExistentResidentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.createTenancy(requestWithBadResident))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentResidentId, TENANT_ID);
            verify(unitRepository, never()).findByIdAndTenantId(any(), any());
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit does not exist")
        void shouldThrowResourceNotFoundExceptionWhenUnitNotFound() {
            UUID nonExistentUnitId = UUID.randomUUID();
            CreateTenancyRequest requestWithBadUnit = CreateTenancyRequest.builder()
                    .residentId(RESIDENT_ID)
                    .unitId(nonExistentUnitId)
                    .tenancyType(TenancyType.TENANT)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(nonExistentUnitId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.createTenancy(requestWithBadUnit))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(unitRepository).findByIdAndTenantId(nonExistentUnitId, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when active OWNER tenancy already exists")
        void shouldThrowWhenDuplicateActiveOwnerTenancy() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.OWNER, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> tenancyService.createTenancy(createRequest))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("OWNER");

            verify(tenancyRepository).existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.OWNER, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when active TENANT tenancy already exists")
        void shouldThrowWhenDuplicateActiveTenantTenancy() {
            CreateTenancyRequest tenantRequest = CreateTenancyRequest.builder()
                    .residentId(RESIDENT_ID)
                    .unitId(UNIT_ID)
                    .tenancyType(TenancyType.TENANT)
                    .startDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.TENANT, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> tenancyService.createTenancy(tenantRequest))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("TENANT");

            verify(tenancyRepository).existsActiveByUnitIdAndTenancyTypeAndTenantId(UNIT_ID, TenancyType.TENANT, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }
    }

    // ========================================================================
    // getTenancy
    // ========================================================================

    @Nested
    @DisplayName("getTenancy")
    class GetTenancy {

        @Test
        @DisplayName("should return tenancy when found by id and tenant")
        void shouldReturnTenancyWhenFound() {
            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));
            when(tenancyMapper.toResponse(tenancy)).thenReturn(tenancyResponse);

            TenancyResponse result = tenancyService.getTenancy(TENANCY_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TENANCY_ID);
            assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getUnitId()).isEqualTo(UNIT_ID);
            assertThat(result.getTenancyType()).isEqualTo(TenancyType.OWNER);
            assertThat(result.getStatus()).isEqualTo(TenancyStatus.ACTIVE);

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyMapper).toResponse(tenancy);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when tenancy not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(tenancyRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.getTenancy(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tenancy")
                    .hasMessageContaining("id");

            verify(tenancyRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(tenancyMapper, never()).toResponse(any(Tenancy.class));
        }
    }

    // ========================================================================
    // getTenanciesByResident
    // ========================================================================

    @Nested
    @DisplayName("getTenanciesByResident")
    class GetTenanciesByResident {

        @Test
        @DisplayName("should return paginated list of tenancies for resident")
        void shouldReturnPaginatedTenancies() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tenancy> page = new PageImpl<>(List.of(tenancy), pageable, 1);

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(tenancyRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(tenancyMapper.toResponse(tenancy)).thenReturn(tenancyResponse);

            PagedResponse<TenancyResponse> result = tenancyService.getTenanciesByResident(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(TENANCY_ID);
            assertThat(result.getContent().get(0).getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(tenancyRepository).findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no tenancies exist for resident")
        void shouldReturnEmptyPageWhenNoTenancies() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tenancy> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(tenancyRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<TenancyResponse> result = tenancyService.getTenanciesByResident(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident does not exist")
        void shouldThrowResourceNotFoundExceptionWhenResidentNotFound() {
            UUID nonExistentResidentId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(residentRepository.findByIdAndTenantId(nonExistentResidentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.getTenanciesByResident(nonExistentResidentId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentResidentId, TENANT_ID);
            verify(tenancyRepository, never()).findByResidentIdAndTenantId(any(), any(), any());
        }
    }

    // ========================================================================
    // getTenanciesByUnit
    // ========================================================================

    @Nested
    @DisplayName("getTenanciesByUnit")
    class GetTenanciesByUnit {

        @Test
        @DisplayName("should return paginated list of tenancies for unit")
        void shouldReturnPaginatedTenancies() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tenancy> page = new PageImpl<>(List.of(tenancy), pageable, 1);

            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(tenancyMapper.toResponse(tenancy)).thenReturn(tenancyResponse);

            PagedResponse<TenancyResponse> result = tenancyService.getTenanciesByUnit(UNIT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(TENANCY_ID);
            assertThat(result.getContent().get(0).getUnitId()).isEqualTo(UNIT_ID);
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(tenancyRepository).findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no tenancies exist for unit")
        void shouldReturnEmptyPageWhenNoTenancies() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Tenancy> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<TenancyResponse> result = tenancyService.getTenanciesByUnit(UNIT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit does not exist")
        void shouldThrowResourceNotFoundExceptionWhenUnitNotFound() {
            UUID nonExistentUnitId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(unitRepository.findByIdAndTenantId(nonExistentUnitId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.getTenanciesByUnit(nonExistentUnitId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("id");

            verify(unitRepository).findByIdAndTenantId(nonExistentUnitId, TENANT_ID);
            verify(tenancyRepository, never()).findByUnitIdAndTenantId(any(), any(), any());
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(3, 5);
            Page<Tenancy> page = new PageImpl<>(List.of(), pageable, 0);

            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyRepository.findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable))
                    .thenReturn(page);

            tenancyService.getTenanciesByUnit(UNIT_ID, pageable);

            verify(tenancyRepository).findByUnitIdAndTenantId(UNIT_ID, TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // updateTenancy
    // ========================================================================

    @Nested
    @DisplayName("updateTenancy")
    class UpdateTenancy {

        private UpdateTenancyRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateTenancyRequest.builder()
                    .tenancyType(TenancyType.TENANT)
                    .startDate(LocalDate.of(2025, 6, 1))
                    .endDate(LocalDate.of(2026, 5, 31))
                    .leaseReference("LEASE-002")
                    .build();
        }

        @Test
        @DisplayName("should update tenancy successfully when status is ACTIVE")
        void shouldUpdateTenancySuccessfully() {
            Tenancy updatedTenancy = new Tenancy();
            updatedTenancy.setId(TENANCY_ID);
            updatedTenancy.setTenantId(TENANT_ID);
            updatedTenancy.setResidentId(RESIDENT_ID);
            updatedTenancy.setUnitId(UNIT_ID);
            updatedTenancy.setTenancyType(TenancyType.TENANT);
            updatedTenancy.setStartDate(LocalDate.of(2025, 6, 1));
            updatedTenancy.setEndDate(LocalDate.of(2026, 5, 31));
            updatedTenancy.setStatus(TenancyStatus.ACTIVE);
            updatedTenancy.setLeaseReference("LEASE-002");
            updatedTenancy.setActive(true);

            TenancyResponse updatedResponse = TenancyResponse.builder()
                    .id(TENANCY_ID)
                    .residentId(RESIDENT_ID)
                    .unitId(UNIT_ID)
                    .tenancyType(TenancyType.TENANT)
                    .startDate(LocalDate.of(2025, 6, 1))
                    .endDate(LocalDate.of(2026, 5, 31))
                    .status(TenancyStatus.ACTIVE)
                    .leaseReference("LEASE-002")
                    .active(true)
                    .build();

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));
            when(tenancyRepository.save(tenancy)).thenReturn(updatedTenancy);
            when(tenancyMapper.toResponse(updatedTenancy)).thenReturn(updatedResponse);

            TenancyResponse result = tenancyService.updateTenancy(TENANCY_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getTenancyType()).isEqualTo(TenancyType.TENANT);
            assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
            assertThat(result.getLeaseReference()).isEqualTo("LEASE-002");
            assertThat(result.getStatus()).isEqualTo(TenancyStatus.ACTIVE);

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyMapper).updateEntity(updateRequest, tenancy);
            verify(tenancyRepository).save(tenancy);
            verify(tenancyMapper).toResponse(updatedTenancy);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when tenancy not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(tenancyRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.updateTenancy(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tenancy")
                    .hasMessageContaining("id");

            verify(tenancyRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(tenancyMapper, never()).updateEntity(any(), any());
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when tenancy status is TERMINATED")
        void shouldThrowWhenTenancyIsTerminated() {
            tenancy.setStatus(TenancyStatus.TERMINATED);

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));

            assertThatThrownBy(() -> tenancyService.updateTenancy(TENANCY_ID, updateRequest))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("TERMINATED");

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyMapper, never()).updateEntity(any(), any());
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when tenancy status is EXPIRED")
        void shouldThrowWhenTenancyIsExpired() {
            tenancy.setStatus(TenancyStatus.EXPIRED);

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));

            assertThatThrownBy(() -> tenancyService.updateTenancy(TENANCY_ID, updateRequest))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("EXPIRED");

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyMapper, never()).updateEntity(any(), any());
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }
    }

    // ========================================================================
    // terminateTenancy
    // ========================================================================

    @Nested
    @DisplayName("terminateTenancy")
    class TerminateTenancy {

        @Test
        @DisplayName("should terminate tenancy and set unit to VACANT when last active tenancy")
        void shouldTerminateTenancyAndSetUnitToVacant() {
            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));
            when(tenancyRepository.save(tenancy)).thenReturn(tenancy);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Collections.emptyList());
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(tenancyMapper.toResponse(tenancy)).thenReturn(
                    TenancyResponse.builder()
                            .id(TENANCY_ID)
                            .residentId(RESIDENT_ID)
                            .unitId(UNIT_ID)
                            .tenancyType(TenancyType.OWNER)
                            .status(TenancyStatus.TERMINATED)
                            .endDate(LocalDate.now())
                            .active(false)
                            .build()
            );

            TenancyResponse result = tenancyService.terminateTenancy(TENANCY_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TenancyStatus.TERMINATED);
            assertThat(result.isActive()).isFalse();

            // Verify tenancy fields were set
            assertThat(tenancy.getStatus()).isEqualTo(TenancyStatus.TERMINATED);
            assertThat(tenancy.getEndDate()).isEqualTo(LocalDate.now());
            assertThat(tenancy.isActive()).isFalse();

            // Verify unit was set to VACANT
            assertThat(unit.getStatus()).isEqualTo(UnitStatus.VACANT);

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyRepository).save(tenancy);
            verify(tenancyRepository).findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(unitRepository).save(unit);
            verify(tenancyMapper).toResponse(tenancy);
        }

        @Test
        @DisplayName("should terminate tenancy but keep unit OCCUPIED when other active tenancies exist")
        void shouldTerminateButKeepUnitOccupiedWhenOtherActiveTenancies() {
            Tenancy otherActiveTenancy = new Tenancy();
            otherActiveTenancy.setId(UUID.randomUUID());
            otherActiveTenancy.setUnitId(UNIT_ID);
            otherActiveTenancy.setStatus(TenancyStatus.ACTIVE);

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));
            when(tenancyRepository.save(tenancy)).thenReturn(tenancy);
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(List.of(otherActiveTenancy));
            when(tenancyMapper.toResponse(tenancy)).thenReturn(
                    TenancyResponse.builder()
                            .id(TENANCY_ID)
                            .status(TenancyStatus.TERMINATED)
                            .active(false)
                            .build()
            );

            TenancyResponse result = tenancyService.terminateTenancy(TENANCY_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TenancyStatus.TERMINATED);

            // Verify tenancy fields were set
            assertThat(tenancy.getStatus()).isEqualTo(TenancyStatus.TERMINATED);
            assertThat(tenancy.isActive()).isFalse();

            // Unit should NOT be updated since other active tenancies remain
            verify(unitRepository, never()).findByIdAndTenantId(any(), any());
            verify(unitRepository, never()).save(any(Unit.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when tenancy not found for termination")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForTermination() {
            UUID nonExistentId = UUID.randomUUID();
            when(tenancyRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.terminateTenancy(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tenancy")
                    .hasMessageContaining("id");

            verify(tenancyRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when tenancy status is TERMINATED")
        void shouldThrowWhenAlreadyTerminated() {
            tenancy.setStatus(TenancyStatus.TERMINATED);

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));

            assertThatThrownBy(() -> tenancyService.terminateTenancy(TENANCY_ID))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("TERMINATED");

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }

        @Test
        @DisplayName("should throw InvalidStateTransitionException when tenancy status is EXPIRED")
        void shouldThrowWhenExpired() {
            tenancy.setStatus(TenancyStatus.EXPIRED);

            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));

            assertThatThrownBy(() -> tenancyService.terminateTenancy(TENANCY_ID))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("EXPIRED");

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }
    }

    // ========================================================================
    // deleteTenancy
    // ========================================================================

    @Nested
    @DisplayName("deleteTenancy")
    class DeleteTenancy {

        @Test
        @DisplayName("should soft-delete tenancy by setting deleted=true and active=false")
        void shouldSoftDeleteTenancy() {
            when(tenancyRepository.findByIdAndTenantId(TENANCY_ID, TENANT_ID))
                    .thenReturn(Optional.of(tenancy));

            tenancyService.deleteTenancy(TENANCY_ID);

            assertThat(tenancy.isDeleted()).isTrue();
            assertThat(tenancy.isActive()).isFalse();

            verify(tenancyRepository).findByIdAndTenantId(TENANCY_ID, TENANT_ID);
            verify(tenancyRepository).save(tenancy);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when tenancy not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(tenancyRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenancyService.deleteTenancy(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tenancy")
                    .hasMessageContaining("id");

            verify(tenancyRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(tenancyRepository, never()).save(any(Tenancy.class));
        }
    }
}
