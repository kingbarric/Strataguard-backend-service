package com.strataguard.core.dto.chat;

import com.strataguard.core.enums.ChatParticipantRole;
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
public class ChatParticipantResponse {
    private UUID id;
    private UUID residentId;
    private String residentName;
    private ChatParticipantRole role;
    private Instant joinedAt;
    private Instant lastReadAt;
}
