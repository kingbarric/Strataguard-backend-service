package com.estatekit.core.dto.blacklist;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateBlacklistRequest {

    private String name;

    private String phone;

    private String plateNumber;

    @NotBlank(message = "Reason is required")
    private String reason;
}
