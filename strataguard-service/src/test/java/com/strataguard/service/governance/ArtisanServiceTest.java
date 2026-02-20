package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Artisan;
import com.strataguard.core.entity.ArtisanRating;
import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ArtisanMapper;
import com.strataguard.core.util.ArtisanRatingMapper;
import com.strataguard.infrastructure.repository.ArtisanRatingRepository;
import com.strataguard.infrastructure.repository.ArtisanRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtisanServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ARTISAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private ArtisanRepository artisanRepository;
    @Mock private ArtisanRatingRepository ratingRepository;
    @Mock private ArtisanMapper artisanMapper;
    @Mock private ArtisanRatingMapper ratingMapper;

    @InjectMocks
    private ArtisanService artisanService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createArtisan_success() {
        CreateArtisanRequest request = CreateArtisanRequest.builder()
                .estateId(ESTATE_ID)
                .name("John Plumber")
                .phone("+2348012345678")
                .category(ArtisanCategory.PLUMBER)
                .build();

        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        ArtisanResponse expectedResponse = ArtisanResponse.builder().id(ARTISAN_ID).name("John Plumber").build();

        when(artisanMapper.toEntity(request)).thenReturn(artisan);
        when(artisanRepository.save(any(Artisan.class))).thenReturn(artisan);
        when(artisanMapper.toResponse(artisan)).thenReturn(expectedResponse);

        ArtisanResponse result = artisanService.createArtisan(request);

        assertThat(result.getId()).isEqualTo(ARTISAN_ID);
        verify(artisanRepository).save(any(Artisan.class));
    }

    @Test
    void updateArtisan_success() {
        UpdateArtisanRequest request = UpdateArtisanRequest.builder().name("Updated Name").build();
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        ArtisanResponse expectedResponse = ArtisanResponse.builder().id(ARTISAN_ID).name("Updated Name").build();

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(artisanRepository.save(artisan)).thenReturn(artisan);
        when(artisanMapper.toResponse(artisan)).thenReturn(expectedResponse);

        ArtisanResponse result = artisanService.updateArtisan(ARTISAN_ID, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(artisanMapper).updateEntity(request, artisan);
    }

    @Test
    void updateArtisan_notFound() {
        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanService.updateArtisan(ARTISAN_ID, UpdateArtisanRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getArtisan_success() {
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        ArtisanResponse expectedResponse = ArtisanResponse.builder().id(ARTISAN_ID).build();

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(artisanMapper.toResponse(artisan)).thenReturn(expectedResponse);

        ArtisanResponse result = artisanService.getArtisan(ARTISAN_ID);

        assertThat(result.getId()).isEqualTo(ARTISAN_ID);
    }

    @Test
    void getArtisan_notFound() {
        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artisanService.getArtisan(ARTISAN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllArtisans_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Artisan artisan = new Artisan();
        Page<Artisan> page = new PageImpl<>(List.of(artisan), pageable, 1);
        ArtisanResponse response = ArtisanResponse.builder().id(ARTISAN_ID).build();

        when(artisanRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(artisanMapper.toResponse(artisan)).thenReturn(response);

        PagedResponse<ArtisanResponse> result = artisanService.getAllArtisans(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getArtisansByEstate_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Artisan> page = new PageImpl<>(List.of(), pageable, 0);

        when(artisanRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ArtisanResponse> result = artisanService.getArtisansByEstate(ESTATE_ID, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getArtisansByCategory_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Artisan> page = new PageImpl<>(List.of(), pageable, 0);

        when(artisanRepository.findByCategoryAndTenantId(ArtisanCategory.PLUMBER, TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ArtisanResponse> result = artisanService.getArtisansByCategory(ArtisanCategory.PLUMBER, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void searchArtisans_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Artisan> page = new PageImpl<>(List.of(), pageable, 0);

        when(artisanRepository.searchByNameOrSpecialization("plumber", TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ArtisanResponse> result = artisanService.searchArtisans("plumber", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void verifyArtisan_success() {
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        artisan.setVerified(false);
        ArtisanResponse expectedResponse = ArtisanResponse.builder().id(ARTISAN_ID).verified(true).build();

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(artisanRepository.save(artisan)).thenReturn(artisan);
        when(artisanMapper.toResponse(artisan)).thenReturn(expectedResponse);

        ArtisanResponse result = artisanService.verifyArtisan(ARTISAN_ID);

        assertThat(result.isVerified()).isTrue();
        assertThat(artisan.isVerified()).isTrue();
    }

    @Test
    void updateStatus_success() {
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        artisan.setStatus(ArtisanStatus.ACTIVE);
        ArtisanResponse expectedResponse = ArtisanResponse.builder().id(ARTISAN_ID).status(ArtisanStatus.SUSPENDED).build();

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(artisanRepository.save(artisan)).thenReturn(artisan);
        when(artisanMapper.toResponse(artisan)).thenReturn(expectedResponse);

        ArtisanResponse result = artisanService.updateStatus(ARTISAN_ID, ArtisanStatus.SUSPENDED);

        assertThat(result.getStatus()).isEqualTo(ArtisanStatus.SUSPENDED);
    }

    @Test
    void rateArtisan_success() {
        RateArtisanRequest request = RateArtisanRequest.builder().rating(5).review("Excellent work").build();
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);
        artisan.setTotalRating(0);
        artisan.setRatingCount(0);
        artisan.setTotalJobs(0);

        ArtisanRating rating = new ArtisanRating();
        rating.setId(UUID.randomUUID());
        ArtisanRatingResponse expectedResponse = ArtisanRatingResponse.builder().rating(5).build();

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(ratingRepository.save(any(ArtisanRating.class))).thenReturn(rating);
        when(ratingMapper.toResponse(rating)).thenReturn(expectedResponse);

        ArtisanRatingResponse result = artisanService.rateArtisan(ARTISAN_ID, RESIDENT_ID, request);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(artisan.getTotalRating()).isEqualTo(5.0);
        assertThat(artisan.getRatingCount()).isEqualTo(1);
        assertThat(artisan.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    void rateArtisan_duplicateRatingForJob() {
        UUID requestId = UUID.randomUUID();
        RateArtisanRequest request = RateArtisanRequest.builder().rating(5).maintenanceRequestId(requestId).build();
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));
        when(ratingRepository.existsByArtisanIdAndResidentIdAndMaintenanceRequestId(ARTISAN_ID, RESIDENT_ID, requestId, TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> artisanService.rateArtisan(ARTISAN_ID, RESIDENT_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already rated");
    }

    @Test
    void deleteArtisan_success() {
        Artisan artisan = new Artisan();
        artisan.setId(ARTISAN_ID);

        when(artisanRepository.findByIdAndTenantId(ARTISAN_ID, TENANT_ID)).thenReturn(Optional.of(artisan));

        artisanService.deleteArtisan(ARTISAN_ID);

        assertThat(artisan.isDeleted()).isTrue();
        verify(artisanRepository).save(artisan);
    }
}
