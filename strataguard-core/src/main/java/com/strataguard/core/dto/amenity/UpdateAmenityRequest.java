package com.strataguard.core.dto.amenity;

import com.strataguard.core.enums.AmenityType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class UpdateAmenityRequest {
    private String name;
    private String description;
    private AmenityType amenityType;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerSession;
    private Boolean requiresBooking;
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
}
