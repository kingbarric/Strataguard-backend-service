package com.estatekit.service.gate;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.exitpass.ExitPassResponse;
import com.estatekit.core.entity.Vehicle;
import com.estatekit.core.exception.GateAccessDeniedException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.infrastructure.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ExitPassService {

    private final VehicleRepository vehicleRepository;
    private final String secret;
    private final int expirySeconds;

    public ExitPassService(VehicleRepository vehicleRepository,
                           @Value("${gate.exit-pass.secret}") String secret,
                           @Value("${gate.exit-pass.expiry-seconds}") int expirySeconds) {
        this.vehicleRepository = vehicleRepository;
        this.secret = secret;
        this.expirySeconds = expirySeconds;
    }

    public ExitPassResponse generateExitPass(UUID vehicleId) {
        UUID tenantId = TenantContext.requireTenantId();
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));

        // Verify the requesting user owns this vehicle (resident check)
        if (vehicle.getResidentId() == null) {
            throw new GateAccessDeniedException("Vehicle has no associated resident");
        }

        String nonce = UUID.randomUUID().toString();
        long expiresAtMillis = Instant.now().plusSeconds(expirySeconds).toEpochMilli();

        String payload = vehicleId + "|" + vehicle.getResidentId() + "|" + tenantId + "|" + nonce + "|" + expiresAtMillis;
        String signature = sign(payload);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;

        log.info("Generated exit pass for vehicle: {} by user: {} for tenant: {}", vehicleId, currentUserId, tenantId);

        return ExitPassResponse.builder()
                .vehicleId(vehicleId)
                .token(token)
                .expiresAt(Instant.ofEpochMilli(expiresAtMillis))
                .build();
    }

    public boolean validateExitPass(String token, UUID vehicleId, UUID tenantId) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                log.warn("Invalid exit pass token format");
                return false;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String providedSignature = parts[1];

            // Verify signature
            String expectedSignature = sign(payload);
            if (!expectedSignature.equals(providedSignature)) {
                log.warn("Invalid exit pass signature for vehicle: {}", vehicleId);
                return false;
            }

            // Parse payload: vehicleId|residentId|tenantId|nonce|expiresAtMillis
            String[] payloadParts = payload.split("\\|");
            if (payloadParts.length != 5) {
                log.warn("Invalid exit pass payload structure");
                return false;
            }

            UUID tokenVehicleId = UUID.fromString(payloadParts[0]);
            UUID tokenTenantId = UUID.fromString(payloadParts[2]);
            long expiresAtMillis = Long.parseLong(payloadParts[4]);

            // Verify vehicle match
            if (!tokenVehicleId.equals(vehicleId)) {
                log.warn("Exit pass vehicle mismatch: token={}, request={}", tokenVehicleId, vehicleId);
                return false;
            }

            // Verify tenant match
            if (!tokenTenantId.equals(tenantId)) {
                log.warn("Exit pass tenant mismatch");
                return false;
            }

            // Verify expiry
            if (Instant.now().toEpochMilli() > expiresAtMillis) {
                log.warn("Exit pass expired for vehicle: {}", vehicleId);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to validate exit pass: {}", e.getMessage());
            return false;
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
