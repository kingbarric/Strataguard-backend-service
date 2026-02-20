package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.service.governance.AnnouncementService;
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
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Announcement board management")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Create an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaimAsString("preferred_username");
        AnnouncementResponse response = announcementService.createAnnouncement(request, userId, userName != null ? userName : userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Announcement created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Update an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement updated"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER', 'SECURITY_GUARD')")
    @Operation(summary = "Get announcement by ID")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementService.getAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get all announcements (admin view)")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAllAnnouncements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AnnouncementResponse> response = announcementService.getAllAnnouncements(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Get announcements by estate (admin view)")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getAnnouncementsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AnnouncementResponse> response = announcementService.getAnnouncementsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}/active")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER', 'SECURITY_GUARD')")
    @Operation(summary = "Get active announcements for estate")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> getActiveAnnouncements(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<AnnouncementResponse> response = announcementService.getActiveAnnouncements(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Publish an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> publishAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementService.publishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement published"));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Unpublish an announcement")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> unpublishAnnouncement(@PathVariable UUID id) {
        AnnouncementResponse response = announcementService.unpublishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Announcement unpublished"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTATE_ADMIN')")
    @Operation(summary = "Delete an announcement")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable UUID id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Announcement deleted"));
    }
}
