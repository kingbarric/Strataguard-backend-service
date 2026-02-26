package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.notification.NotificationPreferenceRequest;
import com.strataguard.core.dto.notification.NotificationPreferenceResponse;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.notification.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Notification preference management endpoints")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final ResidentRepository residentRepository;

    @GetMapping("/my-preferences")
    @PreAuthorize("hasPermission(null, 'notification.preference_manage')")
    @Operation(summary = "Get current resident's notification preferences")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getMyPreferences(
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        List<NotificationPreferenceResponse> response = preferenceService.getPreferences(residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/my-preferences")
    @PreAuthorize("hasPermission(null, 'notification.preference_manage')")
    @Operation(summary = "Update a notification preference")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updateMyPreference(
            @Valid @RequestBody NotificationPreferenceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        NotificationPreferenceResponse response = preferenceService.updatePreference(residentId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Preference updated successfully"));
    }

    private UUID getResidentIdFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
