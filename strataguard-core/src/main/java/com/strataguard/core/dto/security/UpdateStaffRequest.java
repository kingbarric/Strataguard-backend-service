package com.strataguard.core.dto.security;

import com.strataguard.core.enums.StaffDepartment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateStaffRequest {

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

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
