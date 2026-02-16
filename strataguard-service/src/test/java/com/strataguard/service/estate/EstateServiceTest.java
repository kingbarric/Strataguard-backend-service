package com.strataguard.service.estate;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.estate.CreateEstateRequest;
import com.strataguard.core.dto.estate.EstateResponse;
import com.strataguard.core.dto.estate.UpdateEstateRequest;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.enums.EstateType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.EstateMapper;
import com.strataguard.infrastructure.repository.EstateRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstateServiceTest {

    @Mock
    private EstateRepository estateRepository;

    @Mock
    private EstateMapper estateMapper;

    @InjectMocks
    private EstateService estateService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private Estate estate;
    private EstateResponse estateResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        estate = new Estate();
        estate.setId(ESTATE_ID);
        estate.setTenantId(TENANT_ID);
        estate.setName("Sunrise Gardens");
        estate.setAddress("123 Main Street");
        estate.setCity("Lagos");
        estate.setState("Lagos");
        estate.setCountry("Nigeria");
        estate.setEstateType(EstateType.RESIDENTIAL_ESTATE);
        estate.setDescription("A beautiful residential estate");
        estate.setContactEmail("info@sunrise.com");
        estate.setContactPhone("+2341234567890");
        estate.setTotalUnits(50);
        estate.setActive(true);
        estate.setDeleted(false);
        estate.setCreatedAt(Instant.now());
        estate.setUpdatedAt(Instant.now());

        estateResponse = EstateResponse.builder()
                .id(ESTATE_ID)
                .name("Sunrise Gardens")
                .address("123 Main Street")
                .city("Lagos")
                .state("Lagos")
                .country("Nigeria")
                .estateType(EstateType.RESIDENTIAL_ESTATE)
                .description("A beautiful residential estate")
                .contactEmail("info@sunrise.com")
                .contactPhone("+2341234567890")
                .totalUnits(50)
                .active(true)
                .createdAt(estate.getCreatedAt())
                .updatedAt(estate.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // createEstate
    // ========================================================================

    @Nested
    @DisplayName("createEstate")
    class CreateEstate {

        private CreateEstateRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateEstateRequest.builder()
                    .name("Sunrise Gardens")
                    .address("123 Main Street")
                    .city("Lagos")
                    .state("Lagos")
                    .country("Nigeria")
                    .estateType(EstateType.RESIDENTIAL_ESTATE)
                    .description("A beautiful residential estate")
                    .contactEmail("info@sunrise.com")
                    .contactPhone("+2341234567890")
                    .totalUnits(50)
                    .build();
        }

        @Test
        @DisplayName("should create estate successfully when no duplicate exists")
        void shouldCreateEstateSuccessfully() {
            when(estateRepository.existsByNameAndTenantId("Sunrise Gardens", TENANT_ID))
                    .thenReturn(false);
            when(estateMapper.toEntity(createRequest)).thenReturn(estate);
            when(estateRepository.save(estate)).thenReturn(estate);
            when(estateMapper.toResponse(estate)).thenReturn(estateResponse);

            EstateResponse result = estateService.createEstate(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ESTATE_ID);
            assertThat(result.getName()).isEqualTo("Sunrise Gardens");
            assertThat(result.getEstateType()).isEqualTo(EstateType.RESIDENTIAL_ESTATE);
            assertThat(result.isActive()).isTrue();

            verify(estateRepository).existsByNameAndTenantId("Sunrise Gardens", TENANT_ID);
            verify(estateMapper).toEntity(createRequest);
            verify(estateRepository).save(estate);
            verify(estateMapper).toResponse(estate);
        }

        @Test
        @DisplayName("should set tenant ID from TenantContext on created estate")
        void shouldSetTenantIdFromContext() {
            when(estateRepository.existsByNameAndTenantId("Sunrise Gardens", TENANT_ID))
                    .thenReturn(false);
            when(estateMapper.toEntity(createRequest)).thenReturn(estate);
            when(estateRepository.save(estate)).thenReturn(estate);
            when(estateMapper.toResponse(estate)).thenReturn(estateResponse);

            estateService.createEstate(createRequest);

            assertThat(estate.getTenantId()).isEqualTo(TENANT_ID);
            verify(estateRepository).save(estate);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when estate name already exists for tenant")
        void shouldThrowDuplicateResourceExceptionWhenNameExists() {
            when(estateRepository.existsByNameAndTenantId("Sunrise Gardens", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> estateService.createEstate(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("name")
                    .hasMessageContaining("Sunrise Gardens");

            verify(estateRepository).existsByNameAndTenantId("Sunrise Gardens", TENANT_ID);
            verify(estateRepository, never()).save(any(Estate.class));
            verify(estateMapper, never()).toEntity(any(CreateEstateRequest.class));
        }
    }

    // ========================================================================
    // getEstate
    // ========================================================================

    @Nested
    @DisplayName("getEstate")
    class GetEstate {

        @Test
        @DisplayName("should return estate when found by id and tenant")
        void shouldReturnEstateWhenFound() {
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(estateMapper.toResponse(estate)).thenReturn(estateResponse);

            EstateResponse result = estateService.getEstate(ESTATE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ESTATE_ID);
            assertThat(result.getName()).isEqualTo("Sunrise Gardens");

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(estateMapper).toResponse(estate);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(estateRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> estateService.getEstate(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("id");

            verify(estateRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(estateMapper, never()).toResponse(any(Estate.class));
        }
    }

    // ========================================================================
    // getAllEstates
    // ========================================================================

    @Nested
    @DisplayName("getAllEstates")
    class GetAllEstates {

        @Test
        @DisplayName("should return paginated list of estates for tenant")
        void shouldReturnPaginatedEstates() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Estate> page = new PageImpl<>(List.of(estate), pageable, 1);

            when(estateRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(estateMapper.toResponse(estate)).thenReturn(estateResponse);

            PagedResponse<EstateResponse> result = estateService.getAllEstates(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Sunrise Gardens");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(estateRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no estates exist for tenant")
        void shouldReturnEmptyPageWhenNoEstates() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Estate> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(estateRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<EstateResponse> result = estateService.getAllEstates(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            Page<Estate> page = new PageImpl<>(List.of(), pageable, 0);

            when(estateRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

            estateService.getAllEstates(pageable);

            verify(estateRepository).findAllByTenantId(TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // searchEstates
    // ========================================================================

    @Nested
    @DisplayName("searchEstates")
    class SearchEstates {

        @Test
        @DisplayName("should return matching estates for search term")
        void shouldReturnMatchingEstates() {
            String search = "Sunrise";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Estate> page = new PageImpl<>(List.of(estate), pageable, 1);

            when(estateRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(page);
            when(estateMapper.toResponse(estate)).thenReturn(estateResponse);

            PagedResponse<EstateResponse> result = estateService.searchEstates(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Sunrise Gardens");

            verify(estateRepository).searchByTenantId(TENANT_ID, search, pageable);
        }

        @Test
        @DisplayName("should return empty page when no estates match search")
        void shouldReturnEmptyPageWhenNoMatch() {
            String search = "NonExistent";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Estate> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(estateRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(emptyPage);

            PagedResponse<EstateResponse> result = estateService.searchEstates(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ========================================================================
    // updateEstate
    // ========================================================================

    @Nested
    @DisplayName("updateEstate")
    class UpdateEstate {

        private UpdateEstateRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateEstateRequest.builder()
                    .name("Sunrise Gardens Updated")
                    .address("456 New Street")
                    .city("Abuja")
                    .estateType(EstateType.GATED_COMMUNITY)
                    .contactEmail("updated@sunrise.com")
                    .totalUnits(100)
                    .build();
        }

        @Test
        @DisplayName("should update estate successfully when found")
        void shouldUpdateEstateSuccessfully() {
            Estate updatedEstate = new Estate();
            updatedEstate.setId(ESTATE_ID);
            updatedEstate.setTenantId(TENANT_ID);
            updatedEstate.setName("Sunrise Gardens Updated");
            updatedEstate.setAddress("456 New Street");
            updatedEstate.setCity("Abuja");
            updatedEstate.setEstateType(EstateType.GATED_COMMUNITY);
            updatedEstate.setContactEmail("updated@sunrise.com");
            updatedEstate.setTotalUnits(100);
            updatedEstate.setActive(true);

            EstateResponse updatedResponse = EstateResponse.builder()
                    .id(ESTATE_ID)
                    .name("Sunrise Gardens Updated")
                    .address("456 New Street")
                    .city("Abuja")
                    .estateType(EstateType.GATED_COMMUNITY)
                    .contactEmail("updated@sunrise.com")
                    .totalUnits(100)
                    .active(true)
                    .build();

            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));
            when(estateRepository.save(estate)).thenReturn(updatedEstate);
            when(estateMapper.toResponse(updatedEstate)).thenReturn(updatedResponse);

            EstateResponse result = estateService.updateEstate(ESTATE_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Sunrise Gardens Updated");
            assertThat(result.getAddress()).isEqualTo("456 New Street");
            assertThat(result.getEstateType()).isEqualTo(EstateType.GATED_COMMUNITY);

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(estateMapper).updateEntity(updateRequest, estate);
            verify(estateRepository).save(estate);
            verify(estateMapper).toResponse(updatedEstate);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(estateRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> estateService.updateEstate(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("id");

            verify(estateRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(estateMapper, never()).updateEntity(any(), any());
            verify(estateRepository, never()).save(any(Estate.class));
        }
    }

    // ========================================================================
    // deleteEstate
    // ========================================================================

    @Nested
    @DisplayName("deleteEstate")
    class DeleteEstate {

        @Test
        @DisplayName("should soft-delete estate by setting deleted=true and active=false")
        void shouldSoftDeleteEstate() {
            when(estateRepository.findByIdAndTenantId(ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estate));

            estateService.deleteEstate(ESTATE_ID);

            assertThat(estate.isDeleted()).isTrue();
            assertThat(estate.isActive()).isFalse();

            verify(estateRepository).findByIdAndTenantId(ESTATE_ID, TENANT_ID);
            verify(estateRepository).save(estate);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when estate not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(estateRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> estateService.deleteEstate(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Estate")
                    .hasMessageContaining("id");

            verify(estateRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(estateRepository, never()).save(any(Estate.class));
        }
    }
}
