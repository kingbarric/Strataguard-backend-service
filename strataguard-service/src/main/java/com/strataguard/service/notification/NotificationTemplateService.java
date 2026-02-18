package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.notification.NotificationTemplateRequest;
import com.strataguard.core.dto.notification.NotificationTemplateResponse;
import com.strataguard.core.entity.NotificationTemplate;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.NotificationTemplateMapper;
import com.strataguard.infrastructure.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationTemplateMapper templateMapper;

    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (templateRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("NotificationTemplate", "name", request.getName());
        }

        NotificationTemplate template = templateMapper.toEntity(request);
        template.setTenantId(tenantId);
        template = templateRepository.save(template);

        log.info("Created notification template: {} for tenant: {}", template.getName(), tenantId);
        return templateMapper.toResponse(template);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAll() {
        UUID tenantId = TenantContext.requireTenantId();
        return templateRepository.findAllByTenantId(tenantId).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", id));
        return templateMapper.toResponse(template);
    }

    public NotificationTemplateResponse update(UUID id, NotificationTemplateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", id));

        templateMapper.updateEntity(request, template);
        template = templateRepository.save(template);

        log.info("Updated notification template: {} for tenant: {}", template.getName(), tenantId);
        return templateMapper.toResponse(template);
    }

    public void delete(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", id));

        template.setDeleted(true);
        template.setActive(false);
        templateRepository.save(template);

        log.info("Soft-deleted notification template: {} for tenant: {}", id, tenantId);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getByEstateId(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();
        return templateRepository.findByEstateIdAndTenantId(estateId, tenantId).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    /**
     * Resolve template for a notification type and channel.
     * Resolution order: estate-specific → global → null (use raw title/body from request).
     */
    @Transactional(readOnly = true)
    public String resolveBody(NotificationType type, NotificationChannel channel, UUID estateId,
                              Map<String, String> data) {
        UUID tenantId = TenantContext.requireTenantId();
        NotificationTemplate template = resolveTemplate(type, channel, estateId, tenantId);
        if (template == null) {
            return null;
        }
        return applyTemplate(template.getBodyTemplate(), data);
    }

    @Transactional(readOnly = true)
    public String resolveSubject(NotificationType type, NotificationChannel channel, UUID estateId,
                                 Map<String, String> data) {
        UUID tenantId = TenantContext.requireTenantId();
        NotificationTemplate template = resolveTemplate(type, channel, estateId, tenantId);
        if (template == null || template.getSubjectTemplate() == null) {
            return null;
        }
        return applyTemplate(template.getSubjectTemplate(), data);
    }

    private NotificationTemplate resolveTemplate(NotificationType type, NotificationChannel channel,
                                                  UUID estateId, UUID tenantId) {
        // 1. Try estate-specific + channel-specific
        if (estateId != null) {
            var template = templateRepository.findByNotificationTypeAndChannelAndEstateIdAndTenantId(
                    type, channel, estateId, tenantId);
            if (template.isPresent()) return template.get();

            // 2. Try estate-specific + channel-agnostic
            template = templateRepository.findByNotificationTypeAndChannelIsNullAndEstateIdAndTenantId(
                    type, estateId, tenantId);
            if (template.isPresent()) return template.get();
        }

        // 3. Try global + channel-specific
        var template = templateRepository.findByNotificationTypeAndChannelAndTenantId(type, channel, tenantId);
        if (template.isPresent()) return template.get();

        // 4. Try global + channel-agnostic
        template = templateRepository.findByNotificationTypeAndChannelIsNullAndTenantId(type, tenantId);
        return template.orElse(null);
    }

    private String applyTemplate(String template, Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
