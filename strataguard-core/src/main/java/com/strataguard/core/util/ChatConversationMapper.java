package com.strataguard.core.util;

import com.strataguard.core.dto.chat.ConversationResponse;
import com.strataguard.core.entity.ChatConversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatConversationMapper {

    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "participants", ignore = true)
    ConversationResponse toResponse(ChatConversation conversation);
}
