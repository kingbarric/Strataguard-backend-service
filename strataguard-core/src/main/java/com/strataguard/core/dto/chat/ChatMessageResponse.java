package com.strataguard.core.dto.chat;

import com.strataguard.core.enums.ChatMessageStatus;
import com.strataguard.core.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private String content;
    private ChatMessageType messageType;
    private ChatMessageStatus status;
    private UUID parentMessageId;
    private String attachmentUrl;
    private Instant createdAt;
}
