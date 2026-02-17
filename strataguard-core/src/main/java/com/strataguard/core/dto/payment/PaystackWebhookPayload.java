package com.strataguard.core.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaystackWebhookPayload {

    private String event;
    private PaystackData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaystackData {

        private String reference;
        private String status;
        private Long amount;

        @JsonProperty("paid_at")
        private String paidAt;

        private String channel;
        private Map<String, Object> metadata;
    }
}
