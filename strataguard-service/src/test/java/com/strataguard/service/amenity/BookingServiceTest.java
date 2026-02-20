package com.strataguard.service.amenity;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Amenity;
import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.entity.BookingWaitlist;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.core.enums.BookingStatus;
import com.strataguard.core.enums.WalletTransactionType;
import com.strataguard.core.exception.BookingConflictException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.BookingMapper;
import com.strataguard.infrastructure.repository.AmenityBookingRepository;
import com.strataguard.infrastructure.repository.AmenityRepository;
import com.strataguard.infrastructure.repository.BookingWaitlistRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.billing.WalletService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID AMENITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID WAITLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock
    private AmenityBookingRepository bookingRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private BookingWaitlistRepository waitlistRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(bookingService, "defaultAdvanceDays", 30);
        ReflectionTestUtils.setField(bookingService, "reminderMinutesBefore", 60);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Helper builders ──────────────────────────────────────────────────

    private Amenity buildAmenity(BigDecimal pricePerSession) {
        Amenity amenity = new Amenity();
        amenity.setId(AMENITY_ID);
        amenity.setTenantId(TENANT_ID);
        amenity.setName("Swimming Pool");
        amenity.setStatus(AmenityStatus.ACTIVE);
        amenity.setOpeningTime(LocalTime.of(6, 0));
        amenity.setClosingTime(LocalTime.of(22, 0));
        amenity.setPricePerSession(pricePerSession);
        amenity.setPricePerHour(null);
        amenity.setAdvanceBookingDays(30);
        amenity.setMinBookingDurationHours(1);
        amenity.setMaxBookingDurationHours(4);
        amenity.setCancellationHoursBefore(24);
        return amenity;
    }

    private AmenityBooking buildBooking(BookingStatus status, BigDecimal amountCharged, BigDecimal amountPaid) {
        AmenityBooking booking = new AmenityBooking();
        booking.setId(BOOKING_ID);
        booking.setTenantId(TENANT_ID);
        booking.setBookingReference("BK-ABCD1234");
        booking.setAmenityId(AMENITY_ID);
        booking.setResidentId(RESIDENT_ID);
        booking.setStartTime(Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS));
        booking.setEndTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS));
        booking.setStatus(status);
        booking.setAmountCharged(amountCharged);
        booking.setAmountPaid(amountPaid);
        booking.setNumberOfGuests(2);
        booking.setPurpose("Leisure");
        return booking;
    }

    private Resident buildResident() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        return resident;
    }

    private CreateBookingRequest buildCreateRequest() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS)
                .plusSeconds(8 * 3600); // 08:00 UTC
        Instant end = start.plus(2, ChronoUnit.HOURS); // 10:00 UTC
        return CreateBookingRequest.builder()
                .amenityId(AMENITY_ID)
                .startTime(start)
                .endTime(end)
                .numberOfGuests(2)
                .purpose("Leisure")
                .notes("Bring towels")
                .recurring(false)
                .build();
    }

    private BookingResponse buildBookingResponse(BookingStatus status, BigDecimal amountCharged) {
        return BookingResponse.builder()
                .id(BOOKING_ID)
                .bookingReference("BK-ABCD1234")
                .amenityId(AMENITY_ID)
                .amenityName("Swimming Pool")
                .residentId(RESIDENT_ID)
                .residentName("John Doe")
                .status(status)
                .amountCharged(amountCharged)
                .numberOfGuests(2)
                .purpose("Leisure")
                .build();
    }

    private WaitlistResponse buildWaitlistResponse() {
        return WaitlistResponse.builder()
                .id(WAITLIST_ID)
                .amenityId(AMENITY_ID)
                .residentId(RESIDENT_ID)
                .desiredStartTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .desiredEndTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .notified(false)
                .build();
    }

    private void stubEnrichBookingResponse(AmenityBooking booking, String amenityName) {
        BookingResponse response = BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .amenityId(booking.getAmenityId())
                .residentId(booking.getResidentId())
                .status(booking.getStatus())
                .amountCharged(booking.getAmountCharged())
                .amountPaid(booking.getAmountPaid())
                .numberOfGuests(booking.getNumberOfGuests())
                .purpose(booking.getPurpose())
                .build();
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        if (amenityName == null) {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            when(amenityRepository.findByIdAndTenantId(booking.getAmenityId(), TENANT_ID))
                    .thenReturn(Optional.of(amenity));
        }

        when(residentRepository.findByIdAndTenantId(booking.getResidentId(), TENANT_ID))
                .thenReturn(Optional.of(buildResident()));
    }

    // ── Test groups ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("should create booking for free amenity and return confirmed response")
        void shouldCreateBookingForFreeAmenity() {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            CreateBookingRequest request = buildCreateRequest();
            AmenityBooking savedBooking = buildBooking(BookingStatus.CONFIRMED, BigDecimal.ZERO, null);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findOverlappingBookings(eq(AMENITY_ID), any(), any(), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());
            when(bookingRepository.save(any(AmenityBooking.class))).thenReturn(savedBooking);
            stubEnrichBookingResponse(savedBooking, amenity.getName());

            BookingResponse result = bookingService.createBooking(RESIDENT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(BOOKING_ID);
            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(result.getResidentName()).isEqualTo("John Doe");

            verify(walletService, never()).debit(any(), any(), any(), any(), any(), any());
            verify(bookingRepository).save(any(AmenityBooking.class));
            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should create booking for paid amenity and debit wallet")
        void shouldCreateBookingForPaidAmenityAndDebitWallet() {
            BigDecimal price = new BigDecimal("50.00");
            Amenity amenity = buildAmenity(price);
            CreateBookingRequest request = buildCreateRequest();
            AmenityBooking savedBooking = buildBooking(BookingStatus.CONFIRMED, price, price);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findOverlappingBookings(eq(AMENITY_ID), any(), any(), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());
            when(bookingRepository.save(any(AmenityBooking.class))).thenReturn(savedBooking);
            stubEnrichBookingResponse(savedBooking, amenity.getName());

            BookingResponse result = bookingService.createBooking(RESIDENT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(result.getAmountCharged()).isEqualByComparingTo(price);

            verify(walletService).debit(eq(RESIDENT_ID), eq(price),
                    eq(WalletTransactionType.DEBIT_AMENITY_BOOKING), isNull(),
                    eq("AMENITY_BOOKING"), any(String.class));
            verify(bookingRepository).save(any(AmenityBooking.class));
        }

        @Test
        @DisplayName("should throw BookingConflictException when time slot overlaps")
        void shouldThrowWhenTimeSlotConflicts() {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            CreateBookingRequest request = buildCreateRequest();
            AmenityBooking existingBooking = buildBooking(BookingStatus.CONFIRMED, BigDecimal.ZERO, null);

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findOverlappingBookings(eq(AMENITY_ID), any(), any(), eq(TENANT_ID)))
                    .thenReturn(List.of(existingBooking));

            assertThatThrownBy(() -> bookingService.createBooking(RESIDENT_ID, request))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("already booked");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when amenity is inactive")
        void shouldThrowWhenAmenityInactive() {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            amenity.setStatus(AmenityStatus.INACTIVE);
            CreateBookingRequest request = buildCreateRequest();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));

            assertThatThrownBy(() -> bookingService.createBooking(RESIDENT_ID, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not available for booking");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when end time is before start time")
        void shouldThrowWhenEndBeforeStart() {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS)
                    .plusSeconds(10 * 3600);
            Instant end = start.minus(1, ChronoUnit.HOURS);

            CreateBookingRequest request = CreateBookingRequest.builder()
                    .amenityId(AMENITY_ID)
                    .startTime(start)
                    .endTime(end)
                    .build();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));

            assertThatThrownBy(() -> bookingService.createBooking(RESIDENT_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End time must be after start time");

            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("should cancel booking and process refund when eligible")
        void shouldCancelBookingWithRefund() {
            BigDecimal paidAmount = new BigDecimal("50.00");
            AmenityBooking booking = buildBooking(BookingStatus.CONFIRMED, paidAmount, paidAmount);
            // Set start time far enough in the future to pass cancellation deadline
            booking.setStartTime(Instant.now().plus(3, ChronoUnit.DAYS));
            booking.setEndTime(Instant.now().plus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));

            Amenity amenity = buildAmenity(paidAmount);
            CancelBookingRequest cancelRequest = CancelBookingRequest.builder().reason("Changed plans").build();

            when(bookingRepository.findByIdAndTenantId(BOOKING_ID, TENANT_ID)).thenReturn(Optional.of(booking));
            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.save(any(AmenityBooking.class))).thenReturn(booking);
            when(waitlistRepository.findMatchingWaitlistEntries(eq(AMENITY_ID), any(), any(), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());
            stubEnrichBookingResponse(booking, null);

            BookingResponse result = bookingService.cancelBooking(BOOKING_ID, RESIDENT_ID, cancelRequest);

            assertThat(result).isNotNull();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getCancelledAt()).isNotNull();
            assertThat(booking.getCancellationReason()).isEqualTo("Changed plans");

            verify(walletService).credit(eq(RESIDENT_ID), eq(paidAmount),
                    eq(WalletTransactionType.CREDIT_BOOKING_REFUND), eq(BOOKING_ID),
                    eq("BOOKING_REFUND"), any(String.class));
            verify(bookingRepository).save(booking);
            verify(notificationService).send(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when booking is already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            AmenityBooking booking = buildBooking(BookingStatus.CANCELLED, BigDecimal.ZERO, null);

            when(bookingRepository.findByIdAndTenantId(BOOKING_ID, TENANT_ID)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(BOOKING_ID, RESIDENT_ID, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be cancelled");

            verify(bookingRepository, never()).save(any());
            verify(walletService, never()).credit(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getBooking")
    class GetBooking {

        @Test
        @DisplayName("should return booking response when found")
        void shouldReturnBookingWhenFound() {
            AmenityBooking booking = buildBooking(BookingStatus.CONFIRMED, BigDecimal.ZERO, null);
            stubEnrichBookingResponse(booking, null);

            when(bookingRepository.findByIdAndTenantId(BOOKING_ID, TENANT_ID)).thenReturn(Optional.of(booking));

            BookingResponse result = bookingService.getBooking(BOOKING_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(BOOKING_ID);
            assertThat(result.getResidentName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when booking not found")
        void shouldThrowWhenNotFound() {
            when(bookingRepository.findByIdAndTenantId(BOOKING_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBooking(BOOKING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Booking");
        }
    }

    @Nested
    @DisplayName("getMyBookings")
    class GetMyBookings {

        @Test
        @DisplayName("should return paged bookings for resident")
        void shouldReturnPagedBookings() {
            Pageable pageable = PageRequest.of(0, 10);
            AmenityBooking booking = buildBooking(BookingStatus.CONFIRMED, BigDecimal.ZERO, null);
            Page<AmenityBooking> page = new PageImpl<>(List.of(booking), pageable, 1);

            when(bookingRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable)).thenReturn(page);

            // Stub enrichBookingResponse for toPagedResponse
            BookingResponse response = buildBookingResponse(BookingStatus.CONFIRMED, BigDecimal.ZERO);
            when(bookingMapper.toResponse(booking)).thenReturn(response);
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident()));

            PagedResponse<BookingResponse> result = bookingService.getMyBookings(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAmenityName()).isEqualTo("Swimming Pool");
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("getAvailability")
    class GetAvailability {

        @Test
        @DisplayName("should return available and booked slots for a given date")
        void shouldReturnAvailabilitySlots() {
            LocalDate date = LocalDate.now().plusDays(1);
            Amenity amenity = buildAmenity(BigDecimal.ZERO);

            Instant dayStart = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            Instant dayEnd = date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);

            AmenityBooking existingBooking = buildBooking(BookingStatus.CONFIRMED, BigDecimal.ZERO, null);
            existingBooking.setStartTime(date.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC));
            existingBooking.setEndTime(date.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC));

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(bookingRepository.findByAmenityIdAndDateAndTenantId(AMENITY_ID, dayStart, dayEnd, TENANT_ID))
                    .thenReturn(List.of(existingBooking));

            AvailabilityResponse result = bookingService.getAvailability(AMENITY_ID, date);

            assertThat(result).isNotNull();
            assertThat(result.getDate()).isEqualTo(date);
            assertThat(result.getBookedSlots()).hasSize(1);
            assertThat(result.getBookedSlots().get(0).getStartTime()).isEqualTo(existingBooking.getStartTime());
            assertThat(result.getBookedSlots().get(0).getEndTime()).isEqualTo(existingBooking.getEndTime());
            assertThat(result.getAvailableSlots()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("joinWaitlist")
    class JoinWaitlist {

        @Test
        @DisplayName("should add resident to waitlist and return response")
        void shouldJoinWaitlistSuccessfully() {
            Amenity amenity = buildAmenity(BigDecimal.ZERO);
            Instant desiredStart = Instant.now().plus(1, ChronoUnit.DAYS);
            Instant desiredEnd = desiredStart.plus(2, ChronoUnit.HOURS);

            BookingWaitlist savedEntry = new BookingWaitlist();
            savedEntry.setId(WAITLIST_ID);
            savedEntry.setTenantId(TENANT_ID);
            savedEntry.setAmenityId(AMENITY_ID);
            savedEntry.setResidentId(RESIDENT_ID);
            savedEntry.setDesiredStartTime(desiredStart);
            savedEntry.setDesiredEndTime(desiredEnd);

            WaitlistResponse expectedResponse = buildWaitlistResponse();

            when(amenityRepository.findByIdAndTenantId(AMENITY_ID, TENANT_ID)).thenReturn(Optional.of(amenity));
            when(waitlistRepository.save(any(BookingWaitlist.class))).thenReturn(savedEntry);
            when(bookingMapper.toResponse(savedEntry)).thenReturn(expectedResponse);

            WaitlistResponse result = bookingService.joinWaitlist(AMENITY_ID, RESIDENT_ID, desiredStart, desiredEnd);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(WAITLIST_ID);
            assertThat(result.getAmenityId()).isEqualTo(AMENITY_ID);
            assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.isNotified()).isFalse();

            verify(waitlistRepository).save(any(BookingWaitlist.class));
        }
    }
}
