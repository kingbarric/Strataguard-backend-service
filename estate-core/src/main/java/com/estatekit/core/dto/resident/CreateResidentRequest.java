package com.estatekit.core.dto.resident;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateResidentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String emergencyContactName;

    private String emergencyContactPhone;
}
