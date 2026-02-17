package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.billing.CreateLevyTypeRequest;
import com.strataguard.core.dto.billing.LevyTypeResponse;
import com.strataguard.core.dto.billing.UpdateLevyTypeRequest;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.entity.LevyType;
import com.strataguard.core.enums.LevyFrequency;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.LevyTypeMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.LevyTypeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class LevyTypeServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LEVY_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private LevyTypeRepository levyTypeRepository;

    @Mock
    private EstateRepository estateRepository;

    @Mock
    private LevyTypeMapper levyTypeMapper;

    @InjectMocks
    private LevyTypeService levyTypeService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private LevyType buildLevyType() {
        LevyType levyType = new LevyType();
        levyType.setId(LEVY_TYPE_ID);
        levyType.setTenantId(TENANT_ID);
        levyType.setName("Monthly Maintenance");
        levyType.setDescription("Monthly maintenance levy");
        levyType.setAmount(new BigDecimal("500.00"));
        levyType.setFrequency(LevyFrequency.MONTHLY);
        levyType.setEstateId(ESTATE_ID);
        levyType.setCategory("Maintenance");
        levyType.setActive(true);
        return levyType;
    }

    private LevyTypeResponse buildLevyTypeResponse() {
        return LevyTypeResponse.builder()
                .id(LEVY_TYPE_ID)
                .name("Monthly Maintenance")
                .description("Monthly maintenance levy")
                .amount(new BigDecimal("500.00"))
                .frequency(LevyFrequency.MONTHLY)
                .estateId(ESTATE_ID)
                .estateName("Sunrise Estate")
                .category("Maintenance")
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private Estate buildEstate() {
        Estate estate = new Estate();
        estate.setId(ESTATE_ID);
        estate.setTenantId(TENANT_ID);
        estate.setName("Sunrise Estate");
        return estate;
    }

    @Nested
    @DisplayName("createLevyType")
    class CreateLevyType {

        @Test
        @DisplayName("should create levy type successfully")
        void shouldCreateLevyTypeSuccessfully() {
            CreateLevyTypeRequest request = CreateLevyTypeRequest.builder()
                    .name("Monthly Maintenance")
                    .description("Monthly maintenance levy")
                    .amount(new BigDecimal("500.00"))
                    .frequency(LevyFrequency.MONTHLY)
                    .estateId(ESTATE_ID)
                    .category("Maintenance")
                    .build();

            LevyType levyType = buildLevyType();
            Estate estate = buildEstate();
            LevyTypeResponse expectedResponse = buildLevyTypeResponse();

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));
            when(levyTypeRepository.existsByNameAndEstateIdAndTenantId("Monthly Maintenance", ESTATE_ID, TENANT_ID)).thenReturn(false);
            when(levyTypeMapper.toEntity(request)).thenReturn(levyType);
            when(levyTypeRepository.save(any(LevyType.class))).thenReturn(levyType);
            when(levyTypeMapper.toResponse(levyType)).thenReturn(expectedResponse);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            LevyTypeResponse result = levyTypeService.createLevyType(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Monthly Maintenance");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getEstateName()).isEqualTo("Sunrise Estate");
            verify(levyTypeRepository).save(any(LevyType.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate not found")
        void shouldThrowWhenEstateNotFound() {
            CreateLevyTypeRequest request = CreateLevyTypeRequest.builder()
                    .name("Monthly Maintenance")
                    .amount(new BigDecimal("500.00"))
                    .frequency(LevyFrequency.MONTHLY)
                    .estateId(ESTATE_ID)
                    .build();

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> levyTypeService.createLevyType(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate");

            verify(levyTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists")
        void shouldThrowWhenDuplicateName() {
            CreateLevyTypeRequest request = CreateLevyTypeRequest.builder()
                    .name("Monthly Maintenance")
                    .amount(new BigDecimal("500.00"))
                    .frequency(LevyFrequency.MONTHLY)
                    .estateId(ESTATE_ID)
                    .build();

            Estate estate = buildEstate();
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));
            when(levyTypeRepository.existsByNameAndEstateIdAndTenantId("Monthly Maintenance", ESTATE_ID, TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> levyTypeService.createLevyType(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("LevyType");

            verify(levyTypeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getLevyType")
    class GetLevyType {

        @Test
        @DisplayName("should return levy type when found")
        void shouldReturnLevyTypeWhenFound() {
            LevyType levyType = buildLevyType();
            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            Estate estate = buildEstate();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.of(levyType));
            when(levyTypeMapper.toResponse(levyType)).thenReturn(expectedResponse);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            LevyTypeResponse result = levyTypeService.getLevyType(LEVY_TYPE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(LEVY_TYPE_ID);
            assertThat(result.getName()).isEqualTo("Monthly Maintenance");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> levyTypeService.getLevyType(LEVY_TYPE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LevyType");
        }
    }

    @Nested
    @DisplayName("getAllLevyTypes")
    class GetAllLevyTypes {

        @Test
        @DisplayName("should return paged levy types")
        void shouldReturnPagedLevyTypes() {
            Pageable pageable = PageRequest.of(0, 10);
            LevyType levyType = buildLevyType();
            LevyTypeResponse response = buildLevyTypeResponse();
            Estate estate = buildEstate();

            Page<LevyType> page = new PageImpl<>(List.of(levyType), pageable, 1);

            when(levyTypeRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(levyTypeMapper.toResponse(levyType)).thenReturn(response);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            PagedResponse<LevyTypeResponse> result = levyTypeService.getAllLevyTypes(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("getLevyTypesByEstate")
    class GetLevyTypesByEstate {

        @Test
        @DisplayName("should return levy types for a specific estate")
        void shouldReturnLevyTypesByEstate() {
            Pageable pageable = PageRequest.of(0, 10);
            LevyType levyType = buildLevyType();
            LevyTypeResponse response = buildLevyTypeResponse();
            Estate estate = buildEstate();

            Page<LevyType> page = new PageImpl<>(List.of(levyType), pageable, 1);

            when(levyTypeRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable)).thenReturn(page);
            when(levyTypeMapper.toResponse(levyType)).thenReturn(response);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            PagedResponse<LevyTypeResponse> result = levyTypeService.getLevyTypesByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEstateId()).isEqualTo(ESTATE_ID);
        }
    }

    @Nested
    @DisplayName("updateLevyType")
    class UpdateLevyType {

        @Test
        @DisplayName("should update levy type successfully")
        void shouldUpdateLevyTypeSuccessfully() {
            UpdateLevyTypeRequest request = UpdateLevyTypeRequest.builder()
                    .name("Updated Maintenance")
                    .amount(new BigDecimal("600.00"))
                    .build();

            LevyType levyType = buildLevyType();
            LevyType updatedLevyType = buildLevyType();
            updatedLevyType.setName("Updated Maintenance");
            updatedLevyType.setAmount(new BigDecimal("600.00"));

            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            expectedResponse.setName("Updated Maintenance");
            expectedResponse.setAmount(new BigDecimal("600.00"));

            Estate estate = buildEstate();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.of(levyType));
            when(levyTypeRepository.existsByNameAndEstateIdAndTenantId("Updated Maintenance", ESTATE_ID, TENANT_ID)).thenReturn(false);
            when(levyTypeRepository.save(any(LevyType.class))).thenReturn(updatedLevyType);
            when(levyTypeMapper.toResponse(updatedLevyType)).thenReturn(expectedResponse);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            LevyTypeResponse result = levyTypeService.updateLevyType(LEVY_TYPE_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Maintenance");
            verify(levyTypeMapper).updateEntity(request, levyType);
            verify(levyTypeRepository).save(levyType);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when levy type not found")
        void shouldThrowWhenNotFound() {
            UpdateLevyTypeRequest request = UpdateLevyTypeRequest.builder()
                    .name("Updated Maintenance")
                    .build();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> levyTypeService.updateLevyType(LEVY_TYPE_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LevyType");

            verify(levyTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when renamed to existing name")
        void shouldThrowWhenDuplicateNameOnRename() {
            UpdateLevyTypeRequest request = UpdateLevyTypeRequest.builder()
                    .name("Existing Levy Name")
                    .build();

            LevyType levyType = buildLevyType();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.of(levyType));
            when(levyTypeRepository.existsByNameAndEstateIdAndTenantId("Existing Levy Name", ESTATE_ID, TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> levyTypeService.updateLevyType(LEVY_TYPE_ID, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("LevyType");

            verify(levyTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip duplicate check when name is unchanged")
        void shouldSkipDuplicateCheckWhenNameUnchanged() {
            UpdateLevyTypeRequest request = UpdateLevyTypeRequest.builder()
                    .name("Monthly Maintenance") // Same name as existing
                    .amount(new BigDecimal("600.00"))
                    .build();

            LevyType levyType = buildLevyType();
            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            Estate estate = buildEstate();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.of(levyType));
            when(levyTypeRepository.save(any(LevyType.class))).thenReturn(levyType);
            when(levyTypeMapper.toResponse(levyType)).thenReturn(expectedResponse);
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(Optional.of(estate));

            LevyTypeResponse result = levyTypeService.updateLevyType(LEVY_TYPE_ID, request);

            assertThat(result).isNotNull();
            verify(levyTypeRepository, never()).existsByNameAndEstateIdAndTenantId(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteLevyType")
    class DeleteLevyType {

        @Test
        @DisplayName("should soft-delete levy type successfully")
        void shouldSoftDeleteLevyTypeSuccessfully() {
            LevyType levyType = buildLevyType();

            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.of(levyType));
            when(levyTypeRepository.save(any(LevyType.class))).thenReturn(levyType);

            levyTypeService.deleteLevyType(LEVY_TYPE_ID);

            assertThat(levyType.isDeleted()).isTrue();
            assertThat(levyType.isActive()).isFalse();
            verify(levyTypeRepository).save(levyType);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when levy type not found")
        void shouldThrowWhenNotFound() {
            when(levyTypeRepository.findByIdAndTenantId(LEVY_TYPE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> levyTypeService.deleteLevyType(LEVY_TYPE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LevyType");

            verify(levyTypeRepository, never()).save(any());
        }
    }
}
