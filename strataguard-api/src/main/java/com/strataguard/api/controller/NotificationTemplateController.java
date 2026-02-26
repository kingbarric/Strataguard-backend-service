package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.notification.NotificationTemplateRequest;
import com.strataguard.core.dto.notification.NotificationTemplateResponse;
import com.strataguard.service.notification.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Notification template management endpoints")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "Create a notification template")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> create(
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = templateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Template created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "List all notification templates")
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> getAll() {
        List<NotificationTemplateResponse> response = templateService.getAll();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "Get a notification template by ID")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getById(@PathVariable UUID id) {
        NotificationTemplateResponse response = templateService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = templateService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Template updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "Soft-delete a notification template")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasPermission(null, 'notification.template_manage')")
    @Operation(summary = "Get notification templates by estate")
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> getByEstateId(
            @PathVariable UUID estateId) {
        List<NotificationTemplateResponse> response = templateService.getByEstateId(estateId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
