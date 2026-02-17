package com.strataguard.core.dto.payment;

import com.strataguard.core.enums.PaymentMethod;
import com.strataguard.core.enums.PaymentProvider;
import com.strataguard.core.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID invoiceId;
    private String invoiceNumber;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentProvider paymentProvider;
    private String reference;
    private String providerReference;
    private PaymentStatus status;
    private Instant paidAt;
    private String metadata;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
