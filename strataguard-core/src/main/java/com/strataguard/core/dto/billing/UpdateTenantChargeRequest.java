package com.strataguard.core.dto.billing;

import com.strataguard.core.enums.LevyFrequency;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantChargeRequest {

    private String name;

    private String description;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private LevyFrequency frequency;

    private String category;

    private List<Integer> reminderDaysBefore;
}
