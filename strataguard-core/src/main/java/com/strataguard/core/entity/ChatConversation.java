package com.strataguard.core.entity;

import com.strataguard.core.enums.ChatConversationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_chat_conversation_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_chat_conversation_estate_id", columnList = "estate_id"),
        @Index(name = "idx_chat_conversation_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class ChatConversation extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ChatConversationType type;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview", length = 500)
    private String lastMessagePreview;
}
