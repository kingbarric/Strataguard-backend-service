package com.strataguard.core.dto.amenity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancelBookingRequest {
    private String reason;
}
