package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationStatus;
import com.strataguard.core.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID recipientId;
    private NotificationChannel channel;
    private NotificationType type;
    private String title;
    private String body;
    private NotificationStatus status;
    private Instant sentAt;
    private Instant readAt;
    private Instant createdAt;
}
