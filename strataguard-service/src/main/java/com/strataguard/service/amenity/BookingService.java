package com.strataguard.service.amenity;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.entity.Amenity;
import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.entity.BookingWaitlist;
import com.strataguard.core.enums.*;
import com.strataguard.core.exception.BookingConflictException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.BookingMapper;
import com.strataguard.infrastructure.repository.AmenityBookingRepository;
import com.strataguard.infrastructure.repository.AmenityRepository;
import com.strataguard.infrastructure.repository.BookingWaitlistRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.billing.WalletService;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final AmenityBookingRepository bookingRepository;
    private final AmenityRepository amenityRepository;
    private final BookingWaitlistRepository waitlistRepository;
    private final ResidentRepository residentRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    @Value("${amenity.booking.default-advance-days:30}")
    private int defaultAdvanceDays;

    @Value("${amenity.booking.reminder-minutes-before:60}")
    private int reminderMinutesBefore;

    public BookingResponse createBooking(UUID residentId, CreateBookingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Amenity amenity = amenityRepository.findByIdAndTenantId(request.getAmenityId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", request.getAmenityId()));

        validateBookingRequest(amenity, request);

        // Check for conflicts
        List<AmenityBooking> conflicts = bookingRepository.findOverlappingBookings(
                request.getAmenityId(), request.getStartTime(), request.getEndTime(), tenantId);
        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("The requested time slot is already booked");
        }

        // Calculate price
        BigDecimal amount = calculateBookingPrice(amenity, request.getStartTime(), request.getEndTime());

        AmenityBooking booking = new AmenityBooking();
        booking.setTenantId(tenantId);
        booking.setBookingReference(generateBookingReference(tenantId));
        booking.setAmenityId(request.getAmenityId());
        booking.setResidentId(residentId);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setAmountCharged(amount);
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setPurpose(request.getPurpose());
        booking.setNotes(request.getNotes());
        booking.setRecurring(request.isRecurring());
        booking.setRecurrencePattern(request.getRecurrencePattern());
        booking.setRecurrenceEndDate(request.getRecurrenceEndDate());

        // Free amenity → CONFIRMED directly; paid → CONFIRMED (debit wallet)
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            walletService.debit(residentId, amount, WalletTransactionType.DEBIT_AMENITY_BOOKING,
                    null, "AMENITY_BOOKING", "Amenity booking: " + amenity.getName());
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setAmountPaid(amount);
        }

        AmenityBooking saved = bookingRepository.save(booking);
        log.info("Created booking: {} for amenity: {} resident: {}", saved.getBookingReference(), amenity.getName(), residentId);

        // Generate recurring bookings if applicable
        if (request.isRecurring() && request.getRecurrencePattern() != null && request.getRecurrenceEndDate() != null) {
            generateRecurringBookings(saved, amenity, tenantId);
        }

        // Send notification
        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(residentId)
                    .type(NotificationType.BOOKING_CONFIRMED)
                    .title("Booking Confirmed")
                    .body("Your booking for " + amenity.getName() + " has been confirmed. Reference: " + saved.getBookingReference())
                    .data(Map.of("bookingId", saved.getId().toString(), "amenityName", amenity.getName()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation notification: {}", e.getMessage());
        }

        return enrichBookingResponse(saved, amenity.getName());
    }

    public BookingResponse cancelBooking(UUID bookingId, UUID requesterId, CancelBookingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        AmenityBooking booking = bookingRepository.findByIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason(request != null ? request.getReason() : null);

        // Process refund if applicable
        boolean refunded = false;
        if (booking.getAmountPaid() != null && booking.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            Amenity amenity = amenityRepository.findByIdAndTenantId(booking.getAmenityId(), tenantId).orElse(null);
            if (amenity != null && isEligibleForRefund(booking, amenity)) {
                walletService.credit(booking.getResidentId(), booking.getAmountPaid(),
                        WalletTransactionType.CREDIT_BOOKING_REFUND, booking.getId(),
                        "BOOKING_REFUND", "Refund for booking: " + booking.getBookingReference());
                refunded = true;
            }
        }

        AmenityBooking saved = bookingRepository.save(booking);
        log.info("Cancelled booking: {} refunded: {}", bookingId, refunded);

        // Notify waitlist entries
        notifyWaitlist(booking.getAmenityId(), booking.getStartTime(), booking.getEndTime(), tenantId);

        // Send cancellation notification
        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(booking.getResidentId())
                    .type(NotificationType.BOOKING_CANCELLED)
                    .title("Booking Cancelled")
                    .body("Your booking " + booking.getBookingReference() + " has been cancelled." + (refunded ? " A refund has been processed to your wallet." : ""))
                    .data(Map.of("bookingId", booking.getId().toString()))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send booking cancellation notification: {}", e.getMessage());
        }

        return enrichBookingResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId) {
        UUID tenantId = TenantContext.requireTenantId();
        AmenityBooking booking = bookingRepository.findByIdAndTenantId(bookingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        return enrichBookingResponse(booking, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookings(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<AmenityBooking> page = bookingRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getBookingsByAmenity(UUID amenityId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<AmenityBooking> page = bookingRepository.findByAmenityIdAndTenantId(amenityId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID amenityId, LocalDate date) {
        UUID tenantId = TenantContext.requireTenantId();
        Amenity amenity = amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));

        Instant dayStart = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<AmenityBooking> bookings = bookingRepository.findByAmenityIdAndDateAndTenantId(
                amenityId, dayStart, dayEnd, tenantId);

        List<AvailabilityResponse.TimeSlot> bookedSlots = bookings.stream()
                .map(b -> AvailabilityResponse.TimeSlot.builder()
                        .startTime(b.getStartTime())
                        .endTime(b.getEndTime())
                        .build())
                .toList();

        // Generate available slots from operating hours
        List<AvailabilityResponse.TimeSlot> availableSlots = new ArrayList<>();
        if (amenity.getOpeningTime() != null && amenity.getClosingTime() != null) {
            Instant openTime = date.atTime(amenity.getOpeningTime()).toInstant(ZoneOffset.UTC);
            Instant closeTime = date.atTime(amenity.getClosingTime()).toInstant(ZoneOffset.UTC);

            // Simple approach: return full operating window minus booked slots
            availableSlots.add(AvailabilityResponse.TimeSlot.builder()
                    .startTime(openTime)
                    .endTime(closeTime)
                    .build());
        }

        return AvailabilityResponse.builder()
                .date(date)
                .availableSlots(availableSlots)
                .bookedSlots(bookedSlots)
                .build();
    }

    public WaitlistResponse joinWaitlist(UUID amenityId, UUID residentId, Instant desiredStartTime, Instant desiredEndTime) {
        UUID tenantId = TenantContext.requireTenantId();

        amenityRepository.findByIdAndTenantId(amenityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));

        BookingWaitlist entry = new BookingWaitlist();
        entry.setTenantId(tenantId);
        entry.setAmenityId(amenityId);
        entry.setResidentId(residentId);
        entry.setDesiredStartTime(desiredStartTime);
        entry.setDesiredEndTime(desiredEndTime);

        BookingWaitlist saved = waitlistRepository.save(entry);
        log.info("Resident {} joined waitlist for amenity {} at {}", residentId, amenityId, desiredStartTime);
        return bookingMapper.toResponse(saved);
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void completeExpiredBookings() {
        // This runs without tenant context — iterate all tenants
        Instant now = Instant.now();
        List<AmenityBooking> completable = bookingRepository.findCompletableBookings(now, TenantContext.getTenantId() != null ? TenantContext.getTenantId() : UUID.fromString("00000000-0000-0000-0000-000000000000"));
        // Note: In production, this would iterate all tenants. For now, it requires tenant context set by a scheduled task runner.
        for (AmenityBooking booking : completable) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        }
        if (!completable.isEmpty()) {
            log.info("Completed {} expired bookings", completable.size());
        }
    }

    private void validateBookingRequest(Amenity amenity, CreateBookingRequest request) {
        if (amenity.getStatus() != AmenityStatus.ACTIVE) {
            throw new IllegalStateException("Amenity is not available for booking. Status: " + amenity.getStatus());
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Validate advance booking days
        int advanceDays = amenity.getAdvanceBookingDays() != null ? amenity.getAdvanceBookingDays() : defaultAdvanceDays;
        Instant maxAdvance = Instant.now().plus(Duration.ofDays(advanceDays));
        if (request.getStartTime().isAfter(maxAdvance)) {
            throw new IllegalArgumentException("Cannot book more than " + advanceDays + " days in advance");
        }

        // Validate booking duration
        long durationHours = Duration.between(request.getStartTime(), request.getEndTime()).toHours();
        if (amenity.getMinBookingDurationHours() != null && durationHours < amenity.getMinBookingDurationHours()) {
            throw new IllegalArgumentException("Minimum booking duration is " + amenity.getMinBookingDurationHours() + " hours");
        }
        if (amenity.getMaxBookingDurationHours() != null && durationHours > amenity.getMaxBookingDurationHours()) {
            throw new IllegalArgumentException("Maximum booking duration is " + amenity.getMaxBookingDurationHours() + " hours");
        }

        // Validate operating hours
        if (amenity.getOpeningTime() != null && amenity.getClosingTime() != null) {
            LocalTime startLocal = request.getStartTime().atZone(ZoneOffset.UTC).toLocalTime();
            LocalTime endLocal = request.getEndTime().atZone(ZoneOffset.UTC).toLocalTime();
            if (startLocal.isBefore(amenity.getOpeningTime()) || endLocal.isAfter(amenity.getClosingTime())) {
                throw new IllegalArgumentException("Booking must be within operating hours: " +
                        amenity.getOpeningTime() + " - " + amenity.getClosingTime());
            }
        }
    }

    private BigDecimal calculateBookingPrice(Amenity amenity, Instant startTime, Instant endTime) {
        if (amenity.getPricePerSession() != null && amenity.getPricePerSession().compareTo(BigDecimal.ZERO) > 0) {
            return amenity.getPricePerSession();
        }
        if (amenity.getPricePerHour() != null && amenity.getPricePerHour().compareTo(BigDecimal.ZERO) > 0) {
            long hours = Math.max(1, Duration.between(startTime, endTime).toHours());
            return amenity.getPricePerHour().multiply(BigDecimal.valueOf(hours));
        }
        return BigDecimal.ZERO;
    }

    private boolean isEligibleForRefund(AmenityBooking booking, Amenity amenity) {
        if (amenity.getCancellationHoursBefore() == null) {
            return true;
        }
        Instant cancellationDeadline = booking.getStartTime().minus(Duration.ofHours(amenity.getCancellationHoursBefore()));
        return Instant.now().isBefore(cancellationDeadline);
    }

    private void notifyWaitlist(UUID amenityId, Instant startTime, Instant endTime, UUID tenantId) {
        List<BookingWaitlist> entries = waitlistRepository.findMatchingWaitlistEntries(
                amenityId, startTime, endTime, tenantId);
        for (BookingWaitlist entry : entries) {
            entry.setNotified(true);
            entry.setNotifiedAt(Instant.now());
            waitlistRepository.save(entry);

            try {
                notificationService.send(SendNotificationRequest.builder()
                        .recipientId(entry.getResidentId())
                        .type(NotificationType.BOOKING_WAITLIST_AVAILABLE)
                        .title("Slot Available")
                        .body("A slot you were waiting for is now available. Book now before it's taken!")
                        .data(Map.of("amenityId", amenityId.toString()))
                        .build());
            } catch (Exception e) {
                log.warn("Failed to send waitlist notification: {}", e.getMessage());
            }
        }
    }

    private void generateRecurringBookings(AmenityBooking parentBooking, Amenity amenity, UUID tenantId) {
        Duration bookingDuration = Duration.between(parentBooking.getStartTime(), parentBooking.getEndTime());
        LocalDate currentDate = parentBooking.getStartTime().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = parentBooking.getRecurrenceEndDate();

        int weeksIncrement = parentBooking.getRecurrencePattern() == RecurrencePattern.BIWEEKLY ? 2 : 1;
        boolean isMonthly = parentBooking.getRecurrencePattern() == RecurrencePattern.MONTHLY;

        while (true) {
            if (isMonthly) {
                currentDate = currentDate.plusMonths(1);
            } else {
                currentDate = currentDate.plusWeeks(weeksIncrement);
            }

            if (currentDate.isAfter(endDate)) break;

            Instant newStart = currentDate.atTime(parentBooking.getStartTime().atZone(ZoneOffset.UTC).toLocalTime())
                    .toInstant(ZoneOffset.UTC);
            Instant newEnd = newStart.plus(bookingDuration);

            // Check for conflicts before creating
            List<AmenityBooking> conflicts = bookingRepository.findOverlappingBookings(
                    amenity.getId(), newStart, newEnd, tenantId);
            if (!conflicts.isEmpty()) {
                log.debug("Skipping recurring booking on {} due to conflict", currentDate);
                continue;
            }

            AmenityBooking recurring = new AmenityBooking();
            recurring.setTenantId(tenantId);
            recurring.setBookingReference(generateBookingReference(tenantId));
            recurring.setAmenityId(parentBooking.getAmenityId());
            recurring.setResidentId(parentBooking.getResidentId());
            recurring.setStartTime(newStart);
            recurring.setEndTime(newEnd);
            recurring.setStatus(BookingStatus.CONFIRMED);
            recurring.setAmountCharged(parentBooking.getAmountCharged());
            recurring.setAmountPaid(parentBooking.getAmountPaid());
            recurring.setNumberOfGuests(parentBooking.getNumberOfGuests());
            recurring.setPurpose(parentBooking.getPurpose());
            recurring.setRecurring(true);
            recurring.setRecurrencePattern(parentBooking.getRecurrencePattern());
            recurring.setRecurrenceEndDate(parentBooking.getRecurrenceEndDate());
            recurring.setParentBookingId(parentBooking.getId());
            bookingRepository.save(recurring);
        }
    }

    private String generateBookingReference(UUID tenantId) {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BookingResponse enrichBookingResponse(AmenityBooking booking, String amenityName) {
        UUID tenantId = TenantContext.requireTenantId();
        BookingResponse response = bookingMapper.toResponse(booking);

        if (amenityName != null) {
            response.setAmenityName(amenityName);
        } else {
            amenityRepository.findByIdAndTenantId(booking.getAmenityId(), tenantId)
                    .ifPresent(a -> response.setAmenityName(a.getName()));
        }

        residentRepository.findByIdAndTenantId(booking.getResidentId(), tenantId)
                .ifPresent(r -> response.setResidentName(r.getFirstName() + " " + r.getLastName()));

        return response;
    }

    private PagedResponse<BookingResponse> toPagedResponse(Page<AmenityBooking> page) {
        UUID tenantId = TenantContext.requireTenantId();
        return PagedResponse.<BookingResponse>builder()
                .content(page.getContent().stream().map(b -> enrichBookingResponse(b, null)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
