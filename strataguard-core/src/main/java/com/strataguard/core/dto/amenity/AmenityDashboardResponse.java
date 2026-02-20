package com.strataguard.core.dto.amenity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AmenityDashboardResponse {
    private long totalAmenities;
    private long activeAmenities;
    private long totalBookingsToday;
    private long pendingBookings;
    private long confirmedBookings;
}
