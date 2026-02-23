package com.strataguard.core.util;

import com.strataguard.core.dto.chat.ChatMessageResponse;
import com.strataguard.core.dto.chat.SendMessageRequest;
import com.strataguard.core.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "senderName", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.ChatMessageStatus.SENT)")
    @Mapping(target = "messageType", expression = "java(request.getMessageType() != null ? request.getMessageType() : com.strataguard.core.enums.ChatMessageType.TEXT)")
    ChatMessage toEntity(SendMessageRequest request);

    ChatMessageResponse toResponse(ChatMessage message);
}
