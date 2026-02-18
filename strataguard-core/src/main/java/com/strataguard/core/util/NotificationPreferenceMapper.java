package com.strataguard.core.util;

import com.strataguard.core.dto.notification.NotificationPreferenceResponse;
import com.strataguard.core.entity.NotificationPreference;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPreferenceMapper {

    NotificationPreferenceResponse toResponse(NotificationPreference preference);
}
