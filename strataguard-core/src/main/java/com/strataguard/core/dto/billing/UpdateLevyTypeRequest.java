package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateLevyTypeRequest {

    private String name;

    private String description;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private LevyFrequency frequency;

    private String category;
}
