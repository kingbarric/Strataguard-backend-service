package com.strataguard.core.entity;

import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "estate_memberships", indexes = {
    @Index(name = "idx_em_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_em_user_id", columnList = "user_id"),
    @Index(name = "idx_em_estate_id", columnList = "estate_id"),
    @Index(name = "idx_em_user_estate", columnList = "user_id, estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class EstateMembership extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "custom_permissions", columnDefinition = "TEXT[]")
    private String[] customPermissionsGranted;

    @Column(name = "custom_permissions_revoked", columnDefinition = "TEXT[]")
    private String[] customPermissionsRevoked;
}
