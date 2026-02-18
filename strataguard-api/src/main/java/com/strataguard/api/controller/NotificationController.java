package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.*;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final ResidentRepository residentRepository;

    @GetMapping("/my-notifications")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get current resident's notifications")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID residentId = getResidentIdFromJwt(jwt);
        PagedResponse<NotificationResponse> response = notificationService.getMyNotifications(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        UnreadCountResponse response = notificationService.getUnreadCount(residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        NotificationResponse response = notificationService.markAsRead(id, residentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read"));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        notificationService.markAllAsRead(residentId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Send notification to specific resident(s)")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        notificationService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Notification sent successfully"));
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Send notification to all residents in an estate")
    public ResponseEntity<ApiResponse<Void>> sendBulkNotification(
            @Valid @RequestBody BulkNotificationRequest request) {
        notificationService.sendBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Bulk notification sent successfully"));
    }

    private UUID getResidentIdFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
