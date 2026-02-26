package com.strataguard.core.entity;

import com.strataguard.core.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "role_permission_defaults", indexes = {
    @Index(name = "idx_rpd_role", columnList = "role")
})
@Getter
@Setter
@NoArgsConstructor
public class RolePermissionDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "permission", nullable = false)
    private String permission;
}
