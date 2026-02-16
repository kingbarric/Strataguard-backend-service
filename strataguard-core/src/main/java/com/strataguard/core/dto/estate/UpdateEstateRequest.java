package com.strataguard.core.dto.estate;

import com.strataguard.core.enums.EstateType;
import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateEstateRequest {

    private String name;

    private String address;

    private String city;

    private String state;

    private String country;

    private EstateType estateType;

    private String description;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String contactPhone;

    private Integer totalUnits;

    private String settings;

    private Boolean active;
}
