package com.estatekit.core.dto.resident;

import com.estatekit.core.enums.ResidentStatus;
import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateResidentRequest {

    private String firstName;

    private String lastName;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String profilePhotoUrl;

    private ResidentStatus status;

    private Boolean active;
}
