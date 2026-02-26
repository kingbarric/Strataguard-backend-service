package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.governance.PollService;
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
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
@Tag(name = "Polls", description = "Voting and polls management")
public class PollController {

    private final PollService pollService;
    private final ResidentRepository residentRepository;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'poll.create')")
    @Operation(summary = "Create a new poll")
    public ResponseEntity<ApiResponse<PollResponse>> createPoll(
            @Valid @RequestBody CreatePollRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String userName = jwt.getClaimAsString("preferred_username");
        PollResponse response = pollService.createPoll(request, userId, userName != null ? userName : userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Poll created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'poll.read')")
    @Operation(summary = "Get poll by ID")
    public ResponseEntity<ApiResponse<PollResponse>> getPoll(@PathVariable UUID id) {
        PollResponse response = pollService.getPoll(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}")
    @PreAuthorize("hasPermission(null, 'poll.read')")
    @Operation(summary = "Get polls by estate")
    public ResponseEntity<ApiResponse<PagedResponse<PollResponse>>> getPollsByEstate(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<PollResponse> response = pollService.getPollsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/estate/{estateId}/active")
    @PreAuthorize("hasPermission(null, 'poll.read')")
    @Operation(summary = "Get active polls for estate")
    public ResponseEntity<ApiResponse<PagedResponse<PollResponse>>> getActivePolls(
            @PathVariable UUID estateId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<PollResponse> response = pollService.getActivePollsByEstate(estateId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasPermission(null, 'poll.update')")
    @Operation(summary = "Activate a poll")
    public ResponseEntity<ApiResponse<PollResponse>> activatePoll(@PathVariable UUID id) {
        PollResponse response = pollService.activatePoll(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Poll activated"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasPermission(null, 'poll.close')")
    @Operation(summary = "Close a poll")
    public ResponseEntity<ApiResponse<PollResponse>> closePoll(@PathVariable UUID id) {
        PollResponse response = pollService.closePoll(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Poll closed"));
    }

    @PostMapping("/{id}/vote")
    @PreAuthorize("hasPermission(null, 'poll.vote')")
    @Operation(summary = "Cast a vote on a poll")
    public ResponseEntity<ApiResponse<Void>> castVote(
            @PathVariable UUID id,
            @Valid @RequestBody CastVoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentId(jwt);
        pollService.castVote(id, residentId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Vote cast successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'poll.delete')")
    @Operation(summary = "Delete a poll")
    public ResponseEntity<ApiResponse<Void>> deletePoll(@PathVariable UUID id) {
        pollService.deletePoll(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Poll deleted"));
    }

    private UUID getResidentId(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        Resident resident = residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId));
        return resident.getId();
    }
}
