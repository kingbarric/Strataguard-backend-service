package com.strataguard.core.entity;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_templates", indexes = {
        @Index(name = "idx_notif_template_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_notif_template_type", columnList = "notification_type, tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationChannel channel;

    @Column(name = "subject_template", length = 500)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "estate_id")
    private UUID estateId;

    @Column(nullable = false)
    private boolean active = true;
}
