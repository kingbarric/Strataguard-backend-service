package com.strataguard.core.dto.chat;

import com.strataguard.core.enums.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    private String content;

    private ChatMessageType messageType;

    private UUID parentMessageId;

    private String attachmentUrl;
}
