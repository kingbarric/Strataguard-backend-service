package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.membership.*;
import com.strataguard.service.membership.EstateMembershipService;
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
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
@Tag(name = "Memberships", description = "Estate membership management")
public class MembershipController {

    private final EstateMembershipService membershipService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'membership.create')")
    @Operation(summary = "Create a new estate membership")
    public ResponseEntity<ApiResponse<EstateMembershipResponse>> createMembership(
            @Valid @RequestBody CreateMembershipRequest request) {
        EstateMembershipResponse response = membershipService.createMembership(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Membership created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'membership.update')")
    @Operation(summary = "Update a membership (role, status, custom permissions)")
    public ResponseEntity<ApiResponse<EstateMembershipResponse>> updateMembership(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembershipRequest request) {
        EstateMembershipResponse response = membershipService.updateMembership(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Membership updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'membership.delete')")
    @Operation(summary = "Revoke a membership")
    public ResponseEntity<ApiResponse<Void>> revokeMembership(@PathVariable UUID id) {
        membershipService.revokeMembership(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Membership revoked successfully"));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasPermission(null, 'membership.read')")
    @Operation(summary = "Get all estate memberships for a user")
    public ResponseEntity<ApiResponse<UserEstatesResponse>> getUserMemberships(
            @PathVariable String userId) {
        UserEstatesResponse response = membershipService.getUserMemberships(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasPermission(null, 'membership.read')")
    @Operation(summary = "Get all members of an estate")
    public ResponseEntity<ApiResponse<PagedResponse<EstateMembershipResponse>>> getEstateMembers(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<EstateMembershipResponse> response =
            membershipService.getEstateMembers(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my estate memberships (for estate switcher UI)")
    public ResponseEntity<ApiResponse<UserEstatesResponse>> getMyMemberships(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        UserEstatesResponse response = membershipService.getUserMemberships(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
