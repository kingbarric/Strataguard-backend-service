package com.strataguard.core.entity;

import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences", indexes = {
        @Index(name = "idx_notif_pref_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_notif_pref_resident", columnList = "resident_id, tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference extends BaseEntity {

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false)
    private boolean enabled = true;
}
