package com.strataguard.api.controller;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.chat.ChatMessageResponse;
import com.strataguard.core.dto.chat.SendMessageRequest;
import com.strataguard.core.dto.chat.TypingIndicator;
import com.strataguard.core.dto.chat.WebSocketChatMessage;
import com.strataguard.core.enums.ChatMessageType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.ChatParticipantRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.api.config.WebSocketEventListener;
import com.strataguard.service.chat.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService messageService;
    private final ResidentRepository residentRepository;
    private final ChatParticipantRepository participantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener eventListener;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebSocketChatMessage wsMessage, Principal principal,
                            SimpMessageHeaderAccessor headerAccessor) {
        UUID residentId = getResidentIdFromPrincipal(principal);

        SendMessageRequest request = SendMessageRequest.builder()
                .content(wsMessage.getContent())
                .messageType(wsMessage.getMessageType() != null
                        ? ChatMessageType.valueOf(wsMessage.getMessageType())
                        : ChatMessageType.TEXT)
                .parentMessageId(wsMessage.getParentMessageId())
                .attachmentUrl(wsMessage.getAttachmentUrl())
                .build();

        ChatMessageResponse response = messageService.sendMessage(wsMessage.getConversationId(), residentId, request);

        // Broadcast to conversation topic
        messagingTemplate.convertAndSend("/topic/conversation." + wsMessage.getConversationId(), response);

        // Send to individual user queues for participants
        UUID tenantId = TenantContext.requireTenantId();
        var otherParticipants = participantRepository.findOtherParticipants(
                wsMessage.getConversationId(), residentId, tenantId);

        for (var participant : otherParticipants) {
            // Look up the userId for this resident to send to their user queue
            residentRepository.findByIdAndTenantId(participant.getResidentId(), tenantId)
                    .ifPresent(resident -> {
                        if (resident.getUserId() != null) {
                            messagingTemplate.convertAndSendToUser(
                                    resident.getUserId(), "/queue/messages", response);

                            // Send offline notification if user is not connected
                            if (!eventListener.isUserOnline(resident.getUserId())) {
                                messageService.sendOfflineNotifications(
                                        wsMessage.getConversationId(), residentId,
                                        response.getSenderName(), response.getContent());
                            }
                        }
                    });
        }

        log.debug("WebSocket message sent in conversation {} by {}", wsMessage.getConversationId(), residentId);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicator indicator, Principal principal) {
        UUID residentId = getResidentIdFromPrincipal(principal);
        indicator.setResidentId(residentId);

        // Look up resident name
        UUID tenantId = TenantContext.requireTenantId();
        residentRepository.findByIdAndTenantId(residentId, tenantId)
                .ifPresent(r -> indicator.setResidentName(r.getFirstName() + " " + r.getLastName()));

        // Broadcast typing indicator to conversation (not persisted)
        messagingTemplate.convertAndSend(
                "/topic/conversation." + indicator.getConversationId() + ".typing", indicator);
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload java.util.Map<String, String> payload, Principal principal) {
        UUID residentId = getResidentIdFromPrincipal(principal);
        UUID conversationId = UUID.fromString(payload.get("conversationId"));
        messageService.markConversationAsRead(conversationId, residentId);
    }

    private UUID getResidentIdFromPrincipal(Principal principal) {
        JwtAuthenticationToken authToken = (JwtAuthenticationToken) principal;
        Jwt jwt = authToken.getToken();
        String userId = jwt.getSubject();
        UUID tenantId = TenantContext.requireTenantId();
        return residentRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "userId", userId))
                .getId();
    }
}
