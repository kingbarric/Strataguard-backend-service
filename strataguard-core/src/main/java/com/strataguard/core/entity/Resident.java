package com.strataguard.core.entity;

import com.strataguard.core.enums.ResidentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "residents", indexes = {
        @Index(name = "idx_residents_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_residents_user_id", columnList = "user_id"),
        @Index(name = "idx_residents_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class Resident extends BaseEntity {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResidentStatus status = ResidentStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private boolean active = true;
}
