package com.strataguard.core.dto.amenity;

import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.core.enums.AmenityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class AmenityResponse {
    private UUID id;
    private UUID estateId;
    private String name;
    private String description;
    private AmenityType amenityType;
    private AmenityStatus status;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerSession;
    private boolean requiresBooking;
    private Integer maxBookingDurationHours;
    private Integer minBookingDurationHours;
    private Integer advanceBookingDays;
    private Integer cancellationHoursBefore;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String operatingDays;
    private String rules;
    private String photoUrls;
    private String contactInfo;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
