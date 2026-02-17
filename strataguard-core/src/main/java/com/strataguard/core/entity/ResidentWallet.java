package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "resident_wallets", indexes = {
        @Index(name = "idx_resident_wallets_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_resident_wallets_resident_id", columnList = "resident_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallet_resident_tenant", columnNames = {"resident_id", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ResidentWallet extends BaseEntity {

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
}
