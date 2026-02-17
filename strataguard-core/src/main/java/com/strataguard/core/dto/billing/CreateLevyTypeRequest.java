package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreateLevyTypeRequest {

    @NotBlank(message = "Levy name is required")
    private String name;

    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Frequency is required")
    private LevyFrequency frequency;

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    private String category;
}
