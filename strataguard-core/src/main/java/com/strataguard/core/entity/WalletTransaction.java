package com.strataguard.core.entity;

import com.strataguard.core.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_transactions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_wallet_transactions_wallet_id", columnList = "wallet_id")
})
@Getter
@Setter
@NoArgsConstructor
public class WalletTransaction extends BaseEntity {

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private WalletTransactionType transactionType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;
}
