package com.strataguard.core.dto.estate;

import com.strataguard.core.enums.EstateType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateEstateRequest {

    @NotBlank(message = "Estate name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    private String city;

    private String state;

    private String country;

    @NotNull(message = "Estate type is required")
    private EstateType estateType;

    private String description;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String contactPhone;

    private Integer totalUnits;

    private String settings;
}
