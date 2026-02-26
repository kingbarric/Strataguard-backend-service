package com.strataguard.api.controller;

import com.strataguard.core.dto.amenity.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.core.config.TenantContext;
import com.strataguard.service.amenity.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Amenity booking endpoints")
public class BookingController {

    private final BookingService bookingService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'booking.create')")
    @Operation(summary = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentId(jwt);
        BookingResponse response = bookingService.createBooking(residentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Booking created successfully"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(null, 'booking.cancel')")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = getResidentIdOrNull(jwt);
        BookingResponse response = bookingService.cancelBooking(id, requesterId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'booking.read')")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID id) {
        BookingResponse response = bookingService.getBooking(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasPermission(null, 'booking.read')")
    @Operation(summary = "Get my bookings")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentId(jwt);
        PagedResponse<BookingResponse> response = bookingService.getMyBookings(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/amenity/{amenityId}")
    @PreAuthorize("hasPermission(null, 'booking.read')")
    @Operation(summary = "Get bookings by amenity")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookingsByAmenity(
            @PathVariable UUID amenityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<BookingResponse> response = bookingService.getBookingsByAmenity(amenityId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/amenity/{amenityId}/availability")
    @PreAuthorize("hasPermission(null, 'booking.read')")
    @Operation(summary = "Get availability for an amenity on a date")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @PathVariable UUID amenityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AvailabilityResponse response = bookingService.getAvailability(amenityId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/amenity/{amenityId}/waitlist")
    @PreAuthorize("hasPermission(null, 'booking.create')")
    @Operation(summary = "Join waitlist for an amenity slot")
    public ResponseEntity<ApiResponse<WaitlistResponse>> joinWaitlist(
            @PathVariable UUID amenityId,
            @RequestParam Instant desiredStartTime,
            @RequestParam Instant desiredEndTime,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentId(jwt);
        WaitlistResponse response = bookingService.joinWaitlist(amenityId, residentId, desiredStartTime, desiredEndTime);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Added to waitlist"));
    }

    private UUID getResidentId(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId));
        return resident.getId();
    }

    private UUID getResidentIdOrNull(Jwt jwt) {
        try {
            return getResidentId(jwt);
        } catch (Exception e) {
            return null;
        }
    }
}
