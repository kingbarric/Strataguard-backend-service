package com.strataguard.core.entity;

import com.strataguard.core.enums.ChatParticipantRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_participants", indexes = {
        @Index(name = "idx_chat_participant_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_chat_participant_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_chat_participant_resident_id", columnList = "resident_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_participant_conv_resident",
                columnNames = {"conversation_id", "resident_id", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ChatParticipant extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatParticipantRole role = ChatParticipantRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;
}
