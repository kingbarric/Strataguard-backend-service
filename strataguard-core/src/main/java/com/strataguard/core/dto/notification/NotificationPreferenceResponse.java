package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID residentId;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private boolean enabled;
}
