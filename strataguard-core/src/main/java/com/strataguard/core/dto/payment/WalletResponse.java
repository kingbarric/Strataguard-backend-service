package com.strataguard.core.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {

    private UUID id;
    private UUID residentId;
    private String residentName;
    private BigDecimal balance;
}
