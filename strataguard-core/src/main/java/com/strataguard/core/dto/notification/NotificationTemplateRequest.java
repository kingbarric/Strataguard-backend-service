package com.strataguard.core.dto.notification;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    private NotificationChannel channel;

    private String subjectTemplate;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    private UUID estateId;
}
