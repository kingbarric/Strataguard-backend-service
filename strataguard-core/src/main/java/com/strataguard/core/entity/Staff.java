package com.strataguard.core.entity;

import com.strataguard.core.enums.StaffDepartment;
import com.strataguard.core.enums.StaffStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_staff_estate_id", columnList = "estate_id"),
        @Index(name = "idx_staff_department", columnList = "department"),
        @Index(name = "idx_staff_user_id", columnList = "user_id")
})
@Getter
@Setter
public class Staff extends BaseEntity {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private StaffDepartment department;

    @Column(name = "position")
    private String position;

    @Column(name = "badge_number")
    private String badgeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
