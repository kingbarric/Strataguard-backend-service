package com.strataguard.core.util;

import com.strataguard.core.dto.notification.NotificationResponse;
import com.strataguard.core.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
