package com.strataguard.core.dto.payment;

import com.strataguard.core.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class InitiatePaymentRequest {

    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    private PaymentMethod paymentMethod;

    private String callbackUrl;
}
