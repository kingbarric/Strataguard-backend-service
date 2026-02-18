package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SendNotificationRequest {

    private UUID recipientId;

    private List<UUID> recipientIds;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    private Map<String, String> data;

    private List<NotificationChannel> channels;
}
