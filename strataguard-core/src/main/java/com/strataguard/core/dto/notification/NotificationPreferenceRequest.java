package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPreferenceRequest {

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
