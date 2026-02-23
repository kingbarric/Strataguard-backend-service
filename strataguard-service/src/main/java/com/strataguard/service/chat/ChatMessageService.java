package com.strataguard.service.chat;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.chat.ChatMessageResponse;
import com.strataguard.core.dto.chat.SendMessageRequest;
import com.strataguard.core.dto.chat.UnreadChatCountResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.entity.ChatConversation;
import com.strataguard.core.entity.ChatMessage;
import com.strataguard.core.entity.ChatParticipant;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ChatMessageMapper;
import com.strataguard.infrastructure.repository.ChatConversationRepository;
import com.strataguard.infrastructure.repository.ChatMessageRepository;
import com.strataguard.infrastructure.repository.ChatParticipantRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatParticipantRepository participantRepository;
    private final ResidentRepository residentRepository;
    private final ChatMessageMapper messageMapper;
    private final ChatConversationService conversationService;
    private final NotificationService notificationService;

    public ChatMessageResponse sendMessage(UUID conversationId, UUID senderResidentId, SendMessageRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Validate sender is participant
        conversationService.validateParticipant(conversationId, senderResidentId, tenantId);

        // Get sender name
        Resident sender = residentRepository.findByIdAndTenantId(senderResidentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", senderResidentId));
        String senderName = sender.getFirstName() + " " + sender.getLastName();

        // Create message
        ChatMessage message = messageMapper.toEntity(request);
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setSenderId(senderResidentId);
        message.setSenderName(senderName);

        ChatMessage saved = messageRepository.save(message);

        // Update conversation preview
        ChatConversation conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatConversation", "id", conversationId));

        conversation.setLastMessageAt(Instant.now());
        String preview = request.getContent();
        if (preview.length() > 500) {
            preview = preview.substring(0, 497) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        // Auto-mark as read for sender
        participantRepository.updateLastReadAt(conversationId, senderResidentId, tenantId, Instant.now());

        log.debug("Message sent in conversation {} by {}", conversationId, senderResidentId);
        return messageMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getMessageHistory(UUID conversationId, UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        conversationService.validateParticipant(conversationId, residentId, tenantId);

        Page<ChatMessage> page = messageRepository.findByConversationIdAndTenantId(conversationId, tenantId, pageable);

        List<ChatMessageResponse> responses = page.getContent().stream()
                .map(messageMapper::toResponse)
                .toList();

        return PagedResponse.<ChatMessageResponse>builder()
                .content(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public void markConversationAsRead(UUID conversationId, UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();

        conversationService.validateParticipant(conversationId, residentId, tenantId);
        participantRepository.updateLastReadAt(conversationId, residentId, tenantId, Instant.now());

        log.debug("Marked conversation {} as read for resident {}", conversationId, residentId);
    }

    @Transactional(readOnly = true)
    public UnreadChatCountResponse getUnreadCount(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();

        List<UUID> conversationIds = participantRepository.findConversationIdsByResidentIdAndTenantId(residentId, tenantId);
        long totalUnread = 0;

        for (UUID convId : conversationIds) {
            ChatParticipant participant = participantRepository
                    .findByConversationIdAndResidentIdAndTenantId(convId, residentId, tenantId)
                    .orElse(null);

            if (participant != null && participant.getLastReadAt() != null) {
                totalUnread += messageRepository.countUnreadMessages(convId, tenantId, participant.getLastReadAt());
            } else if (participant != null) {
                totalUnread += messageRepository.countAllMessages(convId, tenantId);
            }
        }

        return UnreadChatCountResponse.builder().count(totalUnread).build();
    }

    public void sendOfflineNotifications(UUID conversationId, UUID senderResidentId, String senderName, String messagePreview) {
        UUID tenantId = TenantContext.requireTenantId();

        List<ChatParticipant> otherParticipants = participantRepository.findOtherParticipants(conversationId, senderResidentId, tenantId);

        for (ChatParticipant participant : otherParticipants) {
            try {
                SendNotificationRequest notifRequest = SendNotificationRequest.builder()
                        .recipientId(participant.getResidentId())
                        .type(NotificationType.CHAT_MESSAGE)
                        .title("New message from " + senderName)
                        .body(messagePreview.length() > 200 ? messagePreview.substring(0, 197) + "..." : messagePreview)
                        .channels(List.of(NotificationChannel.IN_APP))
                        .build();

                notificationService.send(notifRequest);
            } catch (Exception e) {
                log.warn("Failed to send offline notification to resident {}: {}", participant.getResidentId(), e.getMessage());
            }
        }
    }
}
