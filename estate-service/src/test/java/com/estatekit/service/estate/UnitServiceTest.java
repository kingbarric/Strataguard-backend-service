package com.estatekit.service.estate;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.unit.CreateUnitRequest;
import com.estatekit.core.dto.unit.UnitResponse;
import com.estatekit.core.dto.unit.UpdateUnitRequest;
import com.estatekit.core.entity.Estate;
import com.estatekit.core.entity.Unit;
import com.estatekit.core.enums.UnitStatus;
import com.estatekit.core.enums.UnitType;
import com.estatekit.core.exception.DuplicateResourceException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.UnitMapper;
import com.estatekit.infrastructure.repository.EstateRepository;
import com.estatekit.infrastructure.repository.UnitRepository;
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
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private EstateRepository estateRepository;

    @Mock
    private UnitMapper unitMapper;

    @InjectMocks
    private UnitService unitService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private Estate estate;
    private Unit unit;
    private UnitResponse unitResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        estate = new Estate();
        estate.setId(ESTATE_ID);
        estate.setTenantId(TENANT_ID);
        estate.setName("Sunrise Gardens");
        estate.setActive(true);
        estate.setDeleted(false);

        unit = new Unit();
        unit.setId(UNIT_ID);
        unit.setTenantId(TENANT_ID);
        unit.setEstateId(ESTATE_ID);
        unit.setUnitNumber("A-101");
        unit.setBlockOrZone("Block A");
        unit.setUnitType(UnitType.FLAT);
        unit.setFloor(1);
        unit.setStatus(UnitStatus.VACANT);
        unit.setBedrooms(3);
        unit.setBathrooms(2);
        unit.setSquareMeters(120.0);
        unit.setDescription("Spacious 3-bedroom flat");
        unit.setActive(true);
        unit.setDeleted(false);
        unit.setCreatedAt(Instant.now());
        unit.setUpdatedAt(Instant.now());

        unitResponse = UnitResponse.builder()
                .id(UNIT_ID)
                .estateId(ESTATE_ID)
                .unitNumber("A-101")
                .blockOrZone("Block A")
                .unitType(UnitType.FLAT)
                .floor(1)
                .status(UnitStatus.VACANT)
                .bedrooms(3)
                .bathrooms(2)
                .squareMeters(120.0)
                .description("Spacious 3-bedroom flat")
                .active(true)
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // createUnit
    // ========================================================================

    @Nested
    @DisplayName("createUnit")
    class CreateUnit {

        private CreateUnitRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateUnitRequest.builder()
                    .estateId(ESTATE_ID)
                    .unitNumber("A-101")
                    .blockOrZone("Block A")
                    .unitType(UnitType.FLAT)
                    .floor(1)
                    .bedrooms(3)
                    .bathrooms(2)
                    .squareMeters(120.0)
                    .description("Spacious 3-bedroom flat")
                    .build();
        }

        @Test
        @DisplayName("should create unit successfully when estate exists and no duplicate unit number")
        void shouldCreateUnitSuccessfully() {
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.existsByUnitNumberAndEstateIdAndTenantId("A-101", ESTATE_ID, TENANT_ID))
                    .thenReturn(false);
            when(unitMapper.toEntity(createRequest)).thenReturn(unit);
            when(unitRepository.save(unit)).thenReturn(unit);
            when(unitMapper.toResponse(unit)).thenReturn(unitResponse);

            UnitResponse result = unitService.createUnit(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(UNIT_ID);
            assertThat(result.getEstateId()).isEqualTo(ESTATE_ID);
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getUnitType()).isEqualTo(UnitType.FLAT);
            assertThat(result.getStatus()).isEqualTo(UnitStatus.VACANT);
            assertThat(result.isActive()).isTrue();

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(unitRepository).existsByUnitNumberAndEstateIdAndTenantId("A-101", ESTATE_ID, TENANT_ID);
            verify(unitMapper).toEntity(createRequest);
            verify(unitRepository).save(unit);
            verify(unitMapper).toResponse(unit);
        }

        @Test
        @DisplayName("should set tenant ID from TenantContext on created unit")
        void shouldSetTenantIdFromContext() {
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.existsByUnitNumberAndEstateIdAndTenantId("A-101", ESTATE_ID, TENANT_ID))
                    .thenReturn(false);
            when(unitMapper.toEntity(createRequest)).thenReturn(unit);
            when(unitRepository.save(unit)).thenReturn(unit);
            when(unitMapper.toResponse(unit)).thenReturn(unitResponse);

            unitService.createUnit(createRequest);

            assertThat(unit.getTenantId()).isEqualTo(TENANT_ID);
            verify(unitRepository).save(unit);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate does not exist")
        void shouldThrowResourceNotFoundExceptionWhenEstateNotFound() {
            UUID nonExistentEstateId = UUID.randomUUID();
            CreateUnitRequest requestWithBadEstate = CreateUnitRequest.builder()
                    .estateId(nonExistentEstateId)
                    .unitNumber("B-201")
                    .unitType(UnitType.DUPLEX)
                    .build();

            when(estateRepository.findByIdAndTenantId(nonExistentEstateId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> unitService.createUnit(requestWithBadEstate))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("id");

            verify(estateRepository).findByIdAndTenantId(nonExistentEstateId, TENANT_ID);
            verify(unitRepository, never()).existsByUnitNumberAndEstateIdAndTenantId(any(), any(), any());
            verify(unitRepository, never()).save(any(Unit.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when unit number already exists in estate")
        void shouldThrowDuplicateResourceExceptionWhenUnitNumberExists() {
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.existsByUnitNumberAndEstateIdAndTenantId("A-101", ESTATE_ID, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> unitService.createUnit(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("unitNumber")
                    .hasMessageContaining("A-101");

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(unitRepository).existsByUnitNumberAndEstateIdAndTenantId("A-101", ESTATE_ID, TENANT_ID);
            verify(unitRepository, never()).save(any(Unit.class));
            verify(unitMapper, never()).toEntity(any(CreateUnitRequest.class));
        }
    }

    // ========================================================================
    // getUnit
    // ========================================================================

    @Nested
    @DisplayName("getUnit")
    class GetUnit {

        @Test
        @DisplayName("should return unit when found by id and tenant")
        void shouldReturnUnitWhenFound() {
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(unitMapper.toResponse(unit)).thenReturn(unitResponse);

            UnitResponse result = unitService.getUnit(UNIT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(UNIT_ID);
            assertThat(result.getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getEstateId()).isEqualTo(ESTATE_ID);

            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(unitMapper).toResponse(unit);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(unitRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> unitService.getUnit(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("id");

            verify(unitRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(unitMapper, never()).toResponse(any(Unit.class));
        }
    }

    // ========================================================================
    // getUnitsByEstate
    // ========================================================================

    @Nested
    @DisplayName("getUnitsByEstate")
    class GetUnitsByEstate {

        @Test
        @DisplayName("should return paginated list of units for estate and tenant")
        void shouldReturnPaginatedUnits() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Unit> page = new PageImpl<>(List.of(unit), pageable, 1);

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.findAllByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(unitMapper.toResponse(unit)).thenReturn(unitResponse);

            PagedResponse<UnitResponse> result = unitService.getUnitsByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUnitNumber()).isEqualTo("A-101");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(unitRepository).findAllByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no units exist for estate")
        void shouldReturnEmptyPageWhenNoUnits() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Unit> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.findAllByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<UnitResponse> result = unitService.getUnitsByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate does not exist")
        void shouldThrowResourceNotFoundExceptionWhenEstateNotFound() {
            UUID nonExistentEstateId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(estateRepository.findByIdAndTenantId(nonExistentEstateId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> unitService.getUnitsByEstate(nonExistentEstateId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("id");

            verify(estateRepository).findByIdAndTenantId(nonExistentEstateId, TENANT_ID);
            verify(unitRepository, never()).findAllByEstateIdAndTenantId(any(), any(), any());
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(3, 5);
            Page<Unit> page = new PageImpl<>(List.of(), pageable, 0);

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(unitRepository.findAllByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable))
                    .thenReturn(page);

            unitService.getUnitsByEstate(ESTATE_ID, pageable);

            verify(unitRepository).findAllByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // updateUnit
    // ========================================================================

    @Nested
    @DisplayName("updateUnit")
    class UpdateUnit {

        private UpdateUnitRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateUnitRequest.builder()
                    .unitNumber("A-102")
                    .blockOrZone("Block A")
                    .unitType(UnitType.DUPLEX)
                    .floor(2)
                    .status(UnitStatus.OCCUPIED)
                    .bedrooms(4)
                    .bathrooms(3)
                    .squareMeters(180.0)
                    .description("Upgraded duplex unit")
                    .build();
        }

        @Test
        @DisplayName("should update unit successfully when found")
        void shouldUpdateUnitSuccessfully() {
            Unit updatedUnit = new Unit();
            updatedUnit.setId(UNIT_ID);
            updatedUnit.setTenantId(TENANT_ID);
            updatedUnit.setEstateId(ESTATE_ID);
            updatedUnit.setUnitNumber("A-102");
            updatedUnit.setBlockOrZone("Block A");
            updatedUnit.setUnitType(UnitType.DUPLEX);
            updatedUnit.setFloor(2);
            updatedUnit.setStatus(UnitStatus.OCCUPIED);
            updatedUnit.setBedrooms(4);
            updatedUnit.setBathrooms(3);
            updatedUnit.setSquareMeters(180.0);
            updatedUnit.setDescription("Upgraded duplex unit");
            updatedUnit.setActive(true);

            UnitResponse updatedResponse = UnitResponse.builder()
                    .id(UNIT_ID)
                    .estateId(ESTATE_ID)
                    .unitNumber("A-102")
                    .blockOrZone("Block A")
                    .unitType(UnitType.DUPLEX)
                    .floor(2)
                    .status(UnitStatus.OCCUPIED)
                    .bedrooms(4)
                    .bathrooms(3)
                    .squareMeters(180.0)
                    .description("Upgraded duplex unit")
                    .active(true)
                    .build();

            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));
            when(unitRepository.save(unit)).thenReturn(updatedUnit);
            when(unitMapper.toResponse(updatedUnit)).thenReturn(updatedResponse);

            UnitResponse result = unitService.updateUnit(UNIT_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getUnitNumber()).isEqualTo("A-102");
            assertThat(result.getUnitType()).isEqualTo(UnitType.DUPLEX);
            assertThat(result.getStatus()).isEqualTo(UnitStatus.OCCUPIED);
            assertThat(result.getBedrooms()).isEqualTo(4);
            assertThat(result.getSquareMeters()).isEqualTo(180.0);

            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(unitMapper).updateEntity(updateRequest, unit);
            verify(unitRepository).save(unit);
            verify(unitMapper).toResponse(updatedUnit);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(unitRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> unitService.updateUnit(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("id");

            verify(unitRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(unitMapper, never()).updateEntity(any(), any());
            verify(unitRepository, never()).save(any(Unit.class));
        }
    }

    // ========================================================================
    // deleteUnit
    // ========================================================================

    @Nested
    @DisplayName("deleteUnit")
    class DeleteUnit {

        @Test
        @DisplayName("should soft-delete unit by setting deleted=true and active=false")
        void shouldSoftDeleteUnit() {
            when(unitRepository.findByIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(Optional.of(unit));

            unitService.deleteUnit(UNIT_ID);

            assertThat(unit.isDeleted()).isTrue();
            assertThat(unit.isActive()).isFalse();

            verify(unitRepository).findByIdAndTenantId(UNIT_ID, TENANT_ID);
            verify(unitRepository).save(unit);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when unit not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(unitRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> unitService.deleteUnit(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Unit")
                    .hasMessageContaining("id");

            verify(unitRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(unitRepository, never()).save(any(Unit.class));
        }
    }

    // ========================================================================
    // countUnitsByEstate
    // ========================================================================

    @Nested
    @DisplayName("countUnitsByEstate")
    class CountUnitsByEstate {

        @Test
        @DisplayName("should return count of units for estate and tenant")
        void shouldReturnUnitCount() {
            when(unitRepository.countByEstateIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(15L);

            long count = unitService.countUnitsByEstate(ESTATE_ID);

            assertThat(count).isEqualTo(15L);

            verify(unitRepository).countByEstateIdAndTenantId(ESTATE_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return zero when no units exist for estate")
        void shouldReturnZeroWhenNoUnits() {
            when(unitRepository.countByEstateIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(0L);

            long count = unitService.countUnitsByEstate(ESTATE_ID);

            assertThat(count).isZero();

            verify(unitRepository).countByEstateIdAndTenantId(ESTATE_ID, TENANT_ID);
        }
    }
}
