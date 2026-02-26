package com.strataguard.api.controller;

import com.strataguard.core.dto.blacklist.BlacklistResponse;
import com.strataguard.core.dto.blacklist.CreateBlacklistRequest;
import com.strataguard.core.dto.blacklist.UpdateBlacklistRequest;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.visitor.BlacklistService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
@Tag(name = "Blacklist", description = "Blacklist management endpoints")
public class BlacklistController {

    private final BlacklistService blacklistService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'blacklist.create')")
    @Operation(summary = "Add a new blacklist entry")
    public ResponseEntity<ApiResponse<BlacklistResponse>> create(
            @Valid @RequestBody CreateBlacklistRequest request) {
        BlacklistResponse response = blacklistService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Blacklist entry created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'blacklist.read')")
    @Operation(summary = "Get all blacklist entries with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<BlacklistResponse> response = blacklistService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'blacklist.read')")
    @Operation(summary = "Get blacklist entry by ID")
    public ResponseEntity<ApiResponse<BlacklistResponse>> getById(@PathVariable UUID id) {
        BlacklistResponse response = blacklistService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'blacklist.update')")
    @Operation(summary = "Update a blacklist entry")
    public ResponseEntity<ApiResponse<BlacklistResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBlacklistRequest request) {
        BlacklistResponse response = blacklistService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Blacklist entry updated successfully"));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasPermission(null, 'blacklist.delete')")
    @Operation(summary = "Deactivate a blacklist entry")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        blacklistService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Blacklist entry deactivated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'blacklist.delete')")
    @Operation(summary = "Delete a blacklist entry")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        blacklistService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Blacklist entry deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasPermission(null, 'blacklist.read')")
    @Operation(summary = "Search blacklist entries")
    public ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> search(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<BlacklistResponse> response = blacklistService.search(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/check")
    @PreAuthorize("hasPermission(null, 'blacklist.check')")
    @Operation(summary = "Check if a phone or plate number is blacklisted")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> check(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String plateNumber) {
        Map<String, Boolean> result = new java.util.HashMap<>();
        if (phone != null && !phone.isBlank()) {
            result.put("phoneBlacklisted", blacklistService.isPhoneBlacklisted(phone));
        }
        if (plateNumber != null && !plateNumber.isBlank()) {
            result.put("plateBlacklisted", blacklistService.isPlateBlacklisted(plateNumber));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
