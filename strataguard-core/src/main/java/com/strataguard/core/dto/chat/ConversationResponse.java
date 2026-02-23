package com.strataguard.core.dto.chat;

import com.strataguard.core.enums.ChatConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID id;
    private UUID estateId;
    private String title;
    private ChatConversationType type;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private long unreadCount;
    private List<ChatParticipantResponse> participants;
    private Instant createdAt;
}
