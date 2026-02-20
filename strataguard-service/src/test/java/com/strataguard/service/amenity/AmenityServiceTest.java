package com.strataguard.service.amenity;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Amenity;
import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.core.enums.AmenityType;
import com.strataguard.core.enums.BookingStatus;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AmenityMapper;
import com.strataguard.infrastructure.repository.AmenityBookingRepository;
import com.strataguard.infrastructure.repository.AmenityRepository;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmenityServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AMENITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private AmenityBookingRepository bookingRepository;

    @Mock
    private AmenityMapper amenityMapper;

    @InjectMocks
    private AmenityService amenityService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------------------------------------------------------------
    // Helper builders
    // ---------------------------------------------------------------

    private Amenity buildAmenity() {
        Amenity amenity = new Amenity();
        amenity.setId(AMENITY_ID);
        amenity.setTenantId(TENANT_ID);
        amenity.setEstateId(ESTATE_ID);
        amenity.setName("Swimming Pool");
        amenity.setDescription("Olympic-size swimming pool");
        amenity.setAmenityType(AmenityType.SWIMMING_POOL);
        amenity.setStatus(AmenityStatus.ACTIVE);
        amenity.setCapacity(50);
        amenity.setPricePerHour(new BigDecimal("25.00"));
        amenity.setPricePerSession(new BigDecimal("15.00"));
        amenity.setRequiresBooking(true);
        amenity.setMaxBookingDurationHours(4);
        amenity.setMinBookingDurationHours(1);
        amenity.setAdvanceBookingDays(7);
        amenity.setCancellationHoursBefore(24);
        amenity.setOpeningTime(LocalTime.of(6, 0));
        amenity.setClosingTime(LocalTime.of(22, 0));
        amenity.setOperatingDays("MON,TUE,WED,THU,FRI,SAT,SUN");
        amenity.setContactInfo("+254700000000");
        amenity.setActive(true);
        return amenity;
    }

    private AmenityResponse buildAmenityResponse() {
        return AmenityResponse.builder()
                .id(AMENITY_ID)
                .estateId(ESTATE_ID)
                .name("Swimming Pool")
                .description("Olympic-size swimming pool")
                .amenityType(AmenityType.SWIMMING_POOL)
                .status(AmenityStatus.ACTIVE)
                .capacity(50)
                .pricePerHour(new BigDecimal("25.00"))
                .pricePerSession(new BigDecimal("15.00"))
                .requiresBooking(true)
                .maxBookingDurationHours(4)
                .minBookingDurationHours(1)
                .advanceBookingDays(7)
                .cancellationHoursBefore(24)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(22, 0))
                .operatingDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .contactInfo("+254700000000")
                .active(true)
                .build();
    }

    private CreateAmenityRequest buildCreateRequest() {
        return CreateAmenityRequest.builder()
                .estateId(ESTATE_ID)
                .name("Swimming Pool")
                .description("Olympic-size swimming pool")
                .amenityType(AmenityType.SWIMMING_POOL)
                .capacity(50)
                .pricePerHour(new BigDecimal("25.00"))
                .pricePerSession(new BigDecimal("15.00"))
                .requiresBooking(true)
                .maxBookingDurationHours(4)
                .minBookingDurationHours(1)
                .advanceBookingDays(7)
                .cancellationHoursBefore(24)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(22, 0))
                .operatingDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .contactInfo("+254700000000")
                .build();
    }

    private AmenityBooking buildFutureBooking() {
        AmenityBooking booking = new AmenityBooking();
        booking.setId(BOOKING_ID);
        booking.setTenantId(TENANT_ID);
        booking.setAmenityId(AMENITY_ID);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStartTime(Instant.now().plusSeconds(86400));
        booking.setEndTime(Instant.now().plusSeconds(90000));
        return booking;
    }

    // ---------------------------------------------------------------
    // createAmenity
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createAmenity")
    class CreateAmenity {

        @Test
        @DisplayName("should create amenity successfully")
        void shouldCreateAmenitySuccessfully() {
            CreateAmenityRequest request = buildCreateRequest();
            Amenity amenity = buildAmenity();
            AmenityResponse expectedResponse = buildAmenityResponse();

            when(amenityRepository.existsByNameAndEstateIdAndTenantId("Swimming Pool", ESTATE_ID, TENANT_ID))
                    .thenReturn(false);
            when(amenityMapper.toEntity(request)).thenReturn(amenity);
            when(amenityRepository.save(any(Amenity.class))).thenReturn(amenity);
            when(amenityMapper.toResponse(amenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.createAmenity(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(AMENITY_ID);
            assertThat(result.getName()).isEqualTo("Swimming Pool");
            assertThat(result.getAmenityType()).isEqualTo(AmenityType.SWIMMING_POOL);
            assertThat(result.getEstateId()).isEqualTo(ESTATE_ID);
            verify(amenityRepository).save(any(Amenity.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists")
        void shouldThrowWhenDuplicateName() {
            CreateAmenityRequest request = buildCreateRequest();

            when(amenityRepository.existsByNameAndEstateIdAndTenantId("Swimming Pool", ESTATE_ID, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> amenityService.createAmenity(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Amenity");

            verify(amenityRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // updateAmenity
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("updateAmenity")
    class UpdateAmenity {

        @Test
        @DisplayName("should update amenity successfully")
        void shouldUpdateAmenitySuccessfully() {
            UpdateAmenityRequest request = UpdateAmenityRequest.builder()
                    .name("Updated Pool")
                    .capacity(100)
                    .build();

            Amenity amenity = buildAmenity();
            Amenity updatedAmenity = buildAmenity();
            updatedAmenity.setName("Updated Pool");
            updatedAmenity.setCapacity(100);

            AmenityResponse expectedResponse = buildAmenityResponse();
            expectedResponse.setName("Updated Pool");
            expectedResponse.setCapacity(100);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(amenityRepository.save(any(Amenity.class))).thenReturn(updatedAmenity);
            when(amenityMapper.toResponse(updatedAmenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.updateAmenity(AMENITY_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Pool");
            assertThat(result.getCapacity()).isEqualTo(100);
            verify(amenityMapper).updateEntity(request, amenity);
            verify(amenityRepository).save(amenity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when amenity not found")
        void shouldThrowWhenNotFound() {
            UpdateAmenityRequest request = UpdateAmenityRequest.builder()
                    .name("Updated Pool")
                    .build();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> amenityService.updateAmenity(AMENITY_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Amenity");

            verify(amenityRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getAmenity
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAmenity")
    class GetAmenity {

        @Test
        @DisplayName("should return amenity when found")
        void shouldReturnAmenityWhenFound() {
            Amenity amenity = buildAmenity();
            AmenityResponse expectedResponse = buildAmenityResponse();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(amenityMapper.toResponse(amenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.getAmenity(AMENITY_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(AMENITY_ID);
            assertThat(result.getName()).isEqualTo("Swimming Pool");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> amenityService.getAmenity(AMENITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Amenity");
        }
    }

    // ---------------------------------------------------------------
    // getAllAmenities
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAllAmenities")
    class GetAllAmenities {

        @Test
        @DisplayName("should return paged amenities")
        void shouldReturnPagedAmenities() {
            Pageable pageable = PageRequest.of(0, 10);
            Amenity amenity = buildAmenity();
            AmenityResponse response = buildAmenityResponse();

            Page<Amenity> page = new PageImpl<>(List.of(amenity), pageable, 1);

            when(amenityRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(amenityMapper.toResponse(amenity)).thenReturn(response);

            PagedResponse<AmenityResponse> result = amenityService.getAllAmenities(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // getAmenitiesByEstate
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAmenitiesByEstate")
    class GetAmenitiesByEstate {

        @Test
        @DisplayName("should return amenities for a specific estate")
        void shouldReturnAmenitiesByEstate() {
            Pageable pageable = PageRequest.of(0, 10);
            Amenity amenity = buildAmenity();
            AmenityResponse response = buildAmenityResponse();

            Page<Amenity> page = new PageImpl<>(List.of(amenity), pageable, 1);

            when(amenityRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID, pageable)).thenReturn(page);
            when(amenityMapper.toResponse(amenity)).thenReturn(response);

            PagedResponse<AmenityResponse> result = amenityService.getAmenitiesByEstate(ESTATE_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEstateId()).isEqualTo(ESTATE_ID);
        }
    }

    // ---------------------------------------------------------------
    // updateAmenityStatus
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("updateAmenityStatus")
    class UpdateAmenityStatus {

        @Test
        @DisplayName("should update amenity status successfully")
        void shouldUpdateStatusSuccessfully() {
            Amenity amenity = buildAmenity();
            Amenity savedAmenity = buildAmenity();
            savedAmenity.setStatus(AmenityStatus.INACTIVE);

            AmenityResponse expectedResponse = buildAmenityResponse();
            expectedResponse.setStatus(AmenityStatus.INACTIVE);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(amenityRepository.save(any(Amenity.class))).thenReturn(savedAmenity);
            when(amenityMapper.toResponse(savedAmenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.updateAmenityStatus(AMENITY_ID, AmenityStatus.INACTIVE);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AmenityStatus.INACTIVE);
            verify(amenityRepository).save(amenity);
        }

        @Test
        @DisplayName("should cancel future bookings when status changes to UNDER_MAINTENANCE")
        void shouldCancelFutureBookingsWhenUnderMaintenance() {
            Amenity amenity = buildAmenity();
            amenity.setStatus(AmenityStatus.ACTIVE);

            Amenity savedAmenity = buildAmenity();
            savedAmenity.setStatus(AmenityStatus.UNDER_MAINTENANCE);

            AmenityResponse expectedResponse = buildAmenityResponse();
            expectedResponse.setStatus(AmenityStatus.UNDER_MAINTENANCE);

            AmenityBooking futureBooking = buildFutureBooking();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findUpcomingConfirmedBookings(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(futureBooking));
            when(amenityRepository.save(any(Amenity.class))).thenReturn(savedAmenity);
            when(amenityMapper.toResponse(savedAmenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.updateAmenityStatus(AMENITY_ID, AmenityStatus.UNDER_MAINTENANCE);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AmenityStatus.UNDER_MAINTENANCE);
            assertThat(futureBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(futureBooking.getCancelledAt()).isNotNull();
            assertThat(futureBooking.getCancellationReason()).isEqualTo("Amenity placed under maintenance");
            verify(bookingRepository).save(futureBooking);
        }

        @Test
        @DisplayName("should not cancel bookings when status does not change to UNDER_MAINTENANCE")
        void shouldNotCancelBookingsWhenNotMaintenance() {
            Amenity amenity = buildAmenity();
            amenity.setStatus(AmenityStatus.ACTIVE);

            Amenity savedAmenity = buildAmenity();
            savedAmenity.setStatus(AmenityStatus.INACTIVE);

            AmenityResponse expectedResponse = buildAmenityResponse();
            expectedResponse.setStatus(AmenityStatus.INACTIVE);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(amenityRepository.save(any(Amenity.class))).thenReturn(savedAmenity);
            when(amenityMapper.toResponse(savedAmenity)).thenReturn(expectedResponse);

            AmenityResponse result = amenityService.updateAmenityStatus(AMENITY_ID, AmenityStatus.INACTIVE);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AmenityStatus.INACTIVE);
            verify(bookingRepository, never()).findUpcomingConfirmedBookings(any(), any(), any());
            verify(bookingRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // deleteAmenity
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("deleteAmenity")
    class DeleteAmenity {

        @Test
        @DisplayName("should soft-delete amenity successfully")
        void shouldSoftDeleteAmenitySuccessfully() {
            Amenity amenity = buildAmenity();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findUpcomingConfirmedBookings(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());
            when(amenityRepository.save(any(Amenity.class))).thenReturn(amenity);

            amenityService.deleteAmenity(AMENITY_ID);

            assertThat(amenity.isDeleted()).isTrue();
            assertThat(amenity.isActive()).isFalse();
            verify(amenityRepository).save(amenity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when amenity not found")
        void shouldThrowWhenNotFound() {
            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> amenityService.deleteAmenity(AMENITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Amenity");

            verify(amenityRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // getDashboard
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return dashboard with correct counts")
        void shouldReturnDashboardWithCorrectCounts() {
            when(amenityRepository.countByTenantId(TENANT_ID)).thenReturn(10L);
            when(amenityRepository.countByStatusAndTenantId(AmenityStatus.ACTIVE, TENANT_ID)).thenReturn(8L);
            when(bookingRepository.countTodayBookings(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                    .thenReturn(5L);
            when(bookingRepository.countByStatusAndTenantId(BookingStatus.PENDING, TENANT_ID)).thenReturn(3L);
            when(bookingRepository.countByStatusAndTenantId(BookingStatus.CONFIRMED, TENANT_ID)).thenReturn(12L);

            AmenityDashboardResponse result = amenityService.getDashboard();

            assertThat(result).isNotNull();
            assertThat(result.getTotalAmenities()).isEqualTo(10L);
            assertThat(result.getActiveAmenities()).isEqualTo(8L);
            assertThat(result.getTotalBookingsToday()).isEqualTo(5L);
            assertThat(result.getPendingBookings()).isEqualTo(3L);
            assertThat(result.getConfirmedBookings()).isEqualTo(12L);
        }
    }
}
