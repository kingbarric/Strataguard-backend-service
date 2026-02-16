package com.strataguard.service.visitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class VisitorPassService {

    private final String secret;

    public VisitorPassService(@Value("${visitor.pass.secret}") String secret) {
        this.secret = secret;
    }

    public String generateToken(String passCode, UUID visitorId, UUID tenantId, long validToMillis) {
        String nonce = UUID.randomUUID().toString();
        String payload = passCode + "|" + visitorId + "|" + tenantId + "|" + nonce + "|" + validToMillis;
        String signature = sign(payload);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
    }

    public boolean validateToken(String token, UUID tenantId) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                log.warn("Invalid visitor pass token format");
                return false;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String providedSignature = parts[1];

            String expectedSignature = sign(payload);
            if (!expectedSignature.equals(providedSignature)) {
                log.warn("Invalid visitor pass signature");
                return false;
            }

            String[] payloadParts = payload.split("\\|");
            if (payloadParts.length != 5) {
                log.warn("Invalid visitor pass payload structure");
                return false;
            }

            UUID tokenTenantId = UUID.fromString(payloadParts[2]);
            if (!tokenTenantId.equals(tenantId)) {
                log.warn("Visitor pass tenant mismatch");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to validate visitor pass token: {}", e.getMessage());
            return false;
        }
    }

    public String extractPassCode(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split("\\|");
            return payloadParts[0];
        } catch (Exception e) {
            log.error("Failed to extract pass code from token: {}", e.getMessage());
            return null;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
