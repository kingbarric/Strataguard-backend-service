package com.strataguard.core.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitiatePaymentResponse {

    private String authorizationUrl;
    private String reference;
    private String accessCode;
}
