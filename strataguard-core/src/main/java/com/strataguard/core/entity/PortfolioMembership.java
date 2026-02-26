package com.strataguard.core.entity;

import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "portfolio_memberships", indexes = {
    @Index(name = "idx_pm_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_pm_user_id", columnList = "user_id"),
    @Index(name = "idx_pm_portfolio_id", columnList = "portfolio_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PortfolioMembership extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;
}
