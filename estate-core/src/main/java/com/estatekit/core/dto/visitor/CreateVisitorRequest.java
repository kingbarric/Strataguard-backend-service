package com.estatekit.core.dto.visitor;

import com.estatekit.core.enums.VisitPassType;
import com.estatekit.core.enums.VisitorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
public class CreateVisitorRequest {

    @NotBlank(message = "Visitor name is required")
    private String name;

    private String phone;

    private String email;

    private String purpose;

    @NotNull(message = "Visitor type is required")
    private VisitorType visitorType;

    private String vehiclePlateNumber;

    @NotNull(message = "Pass type is required")
    private VisitPassType passType;

    @NotNull(message = "Valid from date is required")
    private Instant validFrom;

    @NotNull(message = "Valid to date is required")
    private Instant validTo;

    private Integer maxEntries;

    private String recurringDays;

    private LocalTime recurringStartTime;

    private LocalTime recurringEndTime;
}
