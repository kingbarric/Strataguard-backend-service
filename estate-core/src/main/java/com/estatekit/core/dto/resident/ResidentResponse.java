package com.estatekit.core.dto.resident;

import com.estatekit.core.enums.ResidentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ResidentResponse {

    private UUID id;
    private String userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String profilePhotoUrl;
    private ResidentStatus status;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
