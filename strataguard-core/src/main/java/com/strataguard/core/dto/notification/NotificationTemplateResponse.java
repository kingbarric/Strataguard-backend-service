package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationTemplateResponse {

    private UUID id;
    private String name;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private String subjectTemplate;
    private String bodyTemplate;
    private UUID estateId;
    private boolean active;
    private Instant createdAt;
}
