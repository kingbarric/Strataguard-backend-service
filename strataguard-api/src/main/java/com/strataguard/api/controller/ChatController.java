package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.chat.*;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.chat.ChatConversationService;
import com.strataguard.service.chat.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat and messaging endpoints")
public class ChatController {

    private final ChatConversationService conversationService;
    private final ChatMessageService messageService;
    private final ResidentRepository residentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/conversations")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ESTATE_ADMIN', 'FACILITY_MANAGER')")
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        ConversationResponse response = conversationService.createConversation(residentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Conversation created successfully"));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get my conversations")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationResponse>>> getMyConversations(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        PagedResponse<ConversationResponse> response = conversationService.getMyConversations(residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation details")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        ConversationResponse response = conversationService.getConversation(id, residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get message history for a conversation")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getMessageHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        PagedResponse<ChatMessageResponse> response = messageService.getMessageHistory(id, residentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send a message to a conversation")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        ChatMessageResponse response = messageService.sendMessage(id, residentId, request);

        // Broadcast via WebSocket to conversation topic
        messagingTemplate.convertAndSend("/topic/conversation." + id, response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Message sent successfully"));
    }

    @PutMapping("/conversations/{id}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        messageService.markConversationAsRead(id, residentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation marked as read"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread chat message count")
    public ResponseEntity<ApiResponse<UnreadChatCountResponse>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        UUID residentId = getResidentIdFromJwt(jwt);
        UnreadChatCountResponse response = messageService.getUnreadCount(residentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getResidentIdFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
