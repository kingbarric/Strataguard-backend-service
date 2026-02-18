package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.notification.NotificationPreferenceRequest;
import com.strataguard.core.dto.notification.NotificationPreferenceResponse;
import com.strataguard.core.entity.NotificationPreference;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.util.NotificationPreferenceMapper;
import com.strataguard.infrastructure.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationPreferenceMapper preferenceMapper;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return preferenceRepository.findByResidentIdAndTenantId(residentId, tenantId).stream()
                .map(preferenceMapper::toResponse)
                .toList();
    }

    public NotificationPreferenceResponse updatePreference(UUID residentId, NotificationPreferenceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        NotificationPreference preference = preferenceRepository
                .findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                        residentId, request.getChannel(), request.getNotificationType(), tenantId)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setTenantId(tenantId);
                    newPref.setResidentId(residentId);
                    newPref.setChannel(request.getChannel());
                    newPref.setNotificationType(request.getNotificationType());
                    return newPref;
                });

        preference.setEnabled(request.getEnabled());
        preference = preferenceRepository.save(preference);

        log.info("Updated notification preference for resident {} channel {} type {}: enabled={}",
                residentId, request.getChannel(), request.getNotificationType(), request.getEnabled());
        return preferenceMapper.toResponse(preference);
    }

    /**
     * Check if a channel is enabled for a resident + notification type.
     * Default is enabled if no preference record exists.
     * IN_APP is always enabled.
     * ANNOUNCEMENT type is always delivered.
     */
    @Transactional(readOnly = true)
    public boolean isChannelEnabled(UUID residentId, NotificationChannel channel, NotificationType type) {
        // IN_APP is always enabled
        if (channel == NotificationChannel.IN_APP) {
            return true;
        }

        // ANNOUNCEMENT type is always delivered
        if (type == NotificationType.ANNOUNCEMENT) {
            return true;
        }

        UUID tenantId = TenantContext.requireTenantId();
        return preferenceRepository
                .findByResidentIdAndChannelAndNotificationTypeAndTenantId(residentId, channel, type, tenantId)
                .map(NotificationPreference::isEnabled)
                .orElse(true); // Default: enabled
    }
}
