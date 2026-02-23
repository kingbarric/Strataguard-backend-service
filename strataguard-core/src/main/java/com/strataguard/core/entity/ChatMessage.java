package com.strataguard.core.entity;

import com.strataguard.core.enums.ChatMessageStatus;
import com.strataguard.core.enums.ChatMessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_message_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_chat_message_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_chat_message_sender_id", columnList = "sender_id"),
        @Index(name = "idx_chat_message_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType = ChatMessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatMessageStatus status = ChatMessageStatus.SENT;

    @Column(name = "parent_message_id")
    private UUID parentMessageId;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "sender_name")
    private String senderName;
}
