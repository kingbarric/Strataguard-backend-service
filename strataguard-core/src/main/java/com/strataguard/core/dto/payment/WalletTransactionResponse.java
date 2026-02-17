package com.strataguard.core.dto.payment;

import com.strataguard.core.enums.WalletTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WalletTransactionResponse {

    private UUID id;
    private UUID walletId;
    private BigDecimal amount;
    private WalletTransactionType transactionType;
    private UUID referenceId;
    private String referenceType;
    private String description;
    private BigDecimal balanceAfter;
    private Instant createdAt;
}
