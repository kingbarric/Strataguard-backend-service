package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEstateChargeRequest {

    @NotBlank(message = "Charge name is required")
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

    private List<Integer> reminderDaysBefore;
}
