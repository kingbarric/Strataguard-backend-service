package com.strataguard.core.dto.security;

import com.strataguard.core.enums.StaffDepartment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateStaffRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;

    private String email;

    @NotNull(message = "Department is required")
    private StaffDepartment department;

    private String position;

    private String badgeNumber;

    private String userId;

    private String photoUrl;

    private LocalDate hireDate;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String notes;
}
