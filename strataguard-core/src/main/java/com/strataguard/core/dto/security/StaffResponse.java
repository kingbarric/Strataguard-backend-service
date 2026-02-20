package com.strataguard.core.dto.security;

import com.strataguard.core.enums.StaffDepartment;
import com.strataguard.core.enums.StaffStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class StaffResponse {

    private UUID id;
    private UUID estateId;
    private String userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private StaffDepartment department;
    private String position;
    private String badgeNumber;
    private StaffStatus status;
    private String photoUrl;
    private LocalDate hireDate;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
