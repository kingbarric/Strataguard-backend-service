package com.strataguard.core.dto.chat;

import com.strataguard.core.enums.ChatConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "Estate ID is required")
    private UUID estateId;

    private String title;

    @NotNull(message = "Conversation type is required")
    private ChatConversationType type;

    @NotEmpty(message = "At least one participant is required")
    private List<UUID> participantIds;
}
