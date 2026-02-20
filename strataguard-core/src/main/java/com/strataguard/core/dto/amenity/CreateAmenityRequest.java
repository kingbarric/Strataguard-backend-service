package com.strataguard.core.dto.amenity;

import com.strataguard.core.enums.AmenityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class CreateAmenityRequest {
    @NotNull(message = "Estate ID is required")
    private UUID estateId;
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotNull(message = "Amenity type is required")
    private AmenityType amenityType;
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
}
