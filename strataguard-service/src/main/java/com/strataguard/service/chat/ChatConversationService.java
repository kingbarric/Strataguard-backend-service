package com.strataguard.service.chat;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.chat.ChatParticipantResponse;
import com.strataguard.core.dto.chat.ConversationResponse;
import com.strataguard.core.dto.chat.CreateConversationRequest;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.ChatConversation;
import com.strataguard.core.entity.ChatParticipant;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.ChatConversationType;
import com.strataguard.core.enums.ChatParticipantRole;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.exception.UnauthorizedException;
import com.strataguard.core.util.ChatConversationMapper;
import com.strataguard.infrastructure.repository.ChatConversationRepository;
import com.strataguard.infrastructure.repository.ChatMessageRepository;
import com.strataguard.infrastructure.repository.ChatParticipantRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final ResidentRepository residentRepository;
    private final ChatConversationMapper conversationMapper;

    public ConversationResponse createConversation(UUID creatorResidentId, CreateConversationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // For DIRECT conversations, check if one already exists between these two residents
        if (request.getType() == ChatConversationType.DIRECT) {
            if (request.getParticipantIds().size() != 1) {
                throw new IllegalArgumentException("Direct conversations must have exactly one other participant");
            }

            UUID otherResidentId = request.getParticipantIds().get(0);
            List<UUID> creatorConversations = participantRepository.findConversationIdsByResidentIdAndTenantId(creatorResidentId, tenantId);

            for (UUID convId : creatorConversations) {
                ChatConversation existing = conversationRepository.findByIdAndTenantId(convId, tenantId).orElse(null);
                if (existing != null && existing.getType() == ChatConversationType.DIRECT) {
                    boolean otherIsParticipant = participantRepository.isParticipant(convId, otherResidentId, tenantId);
                    if (otherIsParticipant) {
                        log.info("Direct conversation already exists between {} and {}: {}", creatorResidentId, otherResidentId, convId);
                        return enrichConversationResponse(existing, creatorResidentId, tenantId);
                    }
                }
            }
        }

        // Create conversation
        ChatConversation conversation = new ChatConversation();
        conversation.setTenantId(tenantId);
        conversation.setEstateId(request.getEstateId());
        conversation.setTitle(request.getTitle());
        conversation.setType(request.getType());
        ChatConversation saved = conversationRepository.save(conversation);

        // Add creator as OWNER
        addParticipant(saved.getId(), creatorResidentId, ChatParticipantRole.OWNER, tenantId);

        // Add other participants as MEMBER
        for (UUID participantId : request.getParticipantIds()) {
            residentRepository.findByIdAndTenantId(participantId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", participantId));
            addParticipant(saved.getId(), participantId, ChatParticipantRole.MEMBER, tenantId);
        }

        log.info("Created chat conversation: {} type: {} for tenant: {}", saved.getId(), saved.getType(), tenantId);
        return enrichConversationResponse(saved, creatorResidentId, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationResponse> getMyConversations(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        List<UUID> conversationIds = participantRepository.findConversationIdsByResidentIdAndTenantId(residentId, tenantId);
        if (conversationIds.isEmpty()) {
            return PagedResponse.<ConversationResponse>builder()
                    .content(List.of())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        Page<ChatConversation> page = conversationRepository.findByIdInAndTenantId(conversationIds, tenantId, pageable);

        List<ConversationResponse> responses = page.getContent().stream()
                .map(conv -> enrichConversationResponse(conv, residentId, tenantId))
                .toList();

        return PagedResponse.<ConversationResponse>builder()
                .content(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        validateParticipant(conversationId, residentId, tenantId);

        ChatConversation conversation = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatConversation", "id", conversationId));

        return enrichConversationResponse(conversation, residentId, tenantId);
    }

    public void validateParticipant(UUID conversationId, UUID residentId, UUID tenantId) {
        if (!participantRepository.isParticipant(conversationId, residentId, tenantId)) {
            throw new UnauthorizedException("You are not a participant of this conversation");
        }
    }

    private void addParticipant(UUID conversationId, UUID residentId, ChatParticipantRole role, UUID tenantId) {
        ChatParticipant participant = new ChatParticipant();
        participant.setTenantId(tenantId);
        participant.setConversationId(conversationId);
        participant.setResidentId(residentId);
        participant.setRole(role);
        participant.setJoinedAt(Instant.now());
        participantRepository.save(participant);
    }

    private ConversationResponse enrichConversationResponse(ChatConversation conversation, UUID residentId, UUID tenantId) {
        ConversationResponse response = conversationMapper.toResponse(conversation);

        // Compute unread count
        ChatParticipant participant = participantRepository
                .findByConversationIdAndResidentIdAndTenantId(conversation.getId(), residentId, tenantId)
                .orElse(null);

        if (participant != null && participant.getLastReadAt() != null) {
            response.setUnreadCount(messageRepository.countUnreadMessages(conversation.getId(), tenantId, participant.getLastReadAt()));
        } else if (participant != null) {
            response.setUnreadCount(messageRepository.countAllMessages(conversation.getId(), tenantId));
        }

        // Set participants with resident names
        List<ChatParticipant> participants = participantRepository.findByConversationIdAndTenantId(conversation.getId(), tenantId);
        List<ChatParticipantResponse> participantResponses = new ArrayList<>();

        for (ChatParticipant p : participants) {
            String residentName = residentRepository.findByIdAndTenantId(p.getResidentId(), tenantId)
                    .map(r -> r.getFirstName() + " " + r.getLastName())
                    .orElse("Unknown");

            participantResponses.add(ChatParticipantResponse.builder()
                    .id(p.getId())
                    .residentId(p.getResidentId())
                    .residentName(residentName)
                    .role(p.getRole())
                    .joinedAt(p.getJoinedAt())
                    .lastReadAt(p.getLastReadAt())
                    .build());
        }

        response.setParticipants(participantResponses);
        return response;
    }
}
