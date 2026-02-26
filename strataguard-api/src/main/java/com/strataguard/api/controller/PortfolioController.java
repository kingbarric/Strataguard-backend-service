package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.portfolio.*;
import com.strataguard.service.portfolio.PortfolioService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolios", description = "Portfolio management for multi-estate organizations")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'portfolio.create')")
    @Operation(summary = "Create a new portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioResponse response = portfolioService.createPortfolio(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Portfolio created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'portfolio.update')")
    @Operation(summary = "Update a portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        PortfolioResponse response = portfolioService.updatePortfolio(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Portfolio updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'portfolio.read')")
    @Operation(summary = "Get a portfolio by ID")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(@PathVariable UUID id) {
        PortfolioResponse response = portfolioService.getPortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'portfolio.read')")
    @Operation(summary = "List all portfolios for the current tenant")
    public ResponseEntity<ApiResponse<PagedResponse<PortfolioResponse>>> listPortfolios(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<PortfolioResponse> response = portfolioService.listPortfolios(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'portfolio.delete')")
    @Operation(summary = "Delete a portfolio (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable UUID id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Portfolio deleted successfully"));
    }

    // ── Estate Assignment ──

    @PostMapping("/{id}/estates/{estateId}")
    @PreAuthorize("hasPermission(null, 'portfolio.assign_estate')")
    @Operation(summary = "Assign an estate to a portfolio")
    public ResponseEntity<ApiResponse<Void>> assignEstate(
            @PathVariable UUID id, @PathVariable UUID estateId) {
        portfolioService.assignEstateToPortfolio(id, estateId);
        return ResponseEntity.ok(ApiResponse.success(null, "Estate assigned to portfolio"));
    }

    @DeleteMapping("/{id}/estates/{estateId}")
    @PreAuthorize("hasPermission(null, 'portfolio.assign_estate')")
    @Operation(summary = "Remove an estate from a portfolio")
    public ResponseEntity<ApiResponse<Void>> removeEstate(
            @PathVariable UUID id, @PathVariable UUID estateId) {
        portfolioService.removeEstateFromPortfolio(id, estateId);
        return ResponseEntity.ok(ApiResponse.success(null, "Estate removed from portfolio"));
    }

    // ── Portfolio Membership ──

    @PostMapping("/{id}/members")
    @PreAuthorize("hasPermission(null, 'portfolio.update')")
    @Operation(summary = "Add a member to a portfolio")
    public ResponseEntity<ApiResponse<PortfolioMembershipResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddPortfolioMemberRequest request) {
        PortfolioMembershipResponse response = portfolioService.addMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Member added to portfolio"));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasPermission(null, 'portfolio.read')")
    @Operation(summary = "List members of a portfolio")
    public ResponseEntity<ApiResponse<PagedResponse<PortfolioMembershipResponse>>> getMembers(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<PortfolioMembershipResponse> response = portfolioService.getMembers(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/members/{membershipId}")
    @PreAuthorize("hasPermission(null, 'portfolio.update')")
    @Operation(summary = "Remove a member from a portfolio")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id, @PathVariable UUID membershipId) {
        portfolioService.removeMember(id, membershipId);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed from portfolio"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my portfolio memberships")
    public ResponseEntity<ApiResponse<List<PortfolioMembershipResponse>>> getMyPortfolios(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<PortfolioMembershipResponse> response = portfolioService.getMyPortfolios(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
