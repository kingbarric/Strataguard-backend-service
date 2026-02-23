package com.strataguard.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketChatMessage {
    private UUID conversationId;
    private String content;
    private String messageType;
    private UUID parentMessageId;
    private String attachmentUrl;
}
