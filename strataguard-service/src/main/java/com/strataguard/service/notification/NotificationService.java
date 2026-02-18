package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.*;
import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.NotificationMapper;
import com.strataguard.infrastructure.repository.NotificationRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.TenancyRepository;
import com.strataguard.infrastructure.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ResidentRepository residentRepository;
    private final TenancyRepository tenancyRepository;
    private final UnitRepository unitRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateService templateService;
    private final NotificationDispatcher dispatcher;

    public void send(SendNotificationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        List<UUID> recipientIds = new ArrayList<>();
        if (request.getRecipientId() != null) {
            recipientIds.add(request.getRecipientId());
        }
        if (request.getRecipientIds() != null) {
            recipientIds.addAll(request.getRecipientIds());
        }

        // Determine channels: use override list or default to all channels
        List<NotificationChannel> channels = request.getChannels() != null && !request.getChannels().isEmpty()
                ? request.getChannels()
                : List.of(NotificationChannel.values());

        for (UUID recipientId : recipientIds) {
            // Verify resident exists
            residentRepository.findByIdAndTenantId(recipientId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", recipientId));

            for (NotificationChannel channel : channels) {
                // Check preferences
                if (!preferenceService.isChannelEnabled(recipientId, channel, request.getType())) {
                    log.debug("Channel {} disabled for resident {} type {}", channel, recipientId, request.getType());
                    continue;
                }

                // Resolve template content
                String body = templateService.resolveBody(
                        request.getType(), channel, null, request.getData());
                if (body == null) {
                    body = request.getBody();
                }

                String title = templateService.resolveSubject(
                        request.getType(), channel, null, request.getData());
                if (title == null) {
                    title = request.getTitle();
                }

                // Apply data substitutions on the raw request body/title if no template was used
                if (request.getData() != null && !request.getData().isEmpty()) {
                    for (Map.Entry<String, String> entry : request.getData().entrySet()) {
                        body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
                        title = title.replace("{{" + entry.getKey() + "}}", entry.getValue());
                    }
                }

                Notification notification = new Notification();
                notification.setTenantId(tenantId);
                notification.setRecipientId(recipientId);
                notification.setChannel(channel);
                notification.setType(request.getType());
                notification.setTitle(title);
                notification.setBody(body);
                notification.setStatus(NotificationStatus.PENDING);
                notification.setRetryCount(0);
                notification = notificationRepository.save(notification);

                dispatcher.dispatch(notification);
            }
        }

        log.info("Sent notification type {} to {} recipients via {} channels",
                request.getType(), recipientIds.size(), channels.size());
    }

    public void sendBulk(BulkNotificationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Find all residents in the estate through units → tenancies
        List<UUID> residentIds = findResidentIdsByEstateId(request.getEstateId(), tenantId);

        if (residentIds.isEmpty()) {
            log.warn("No residents found in estate {} for bulk notification", request.getEstateId());
            return;
        }

        SendNotificationRequest sendRequest = SendNotificationRequest.builder()
                .recipientIds(residentIds)
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .data(request.getData())
                .build();

        send(sendRequest);
        log.info("Bulk notification sent to {} residents in estate {}", residentIds.size(), request.getEstateId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Notification> page = notificationRepository.findByRecipientIdAndTenantId(
                residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    public NotificationResponse markAsRead(UUID notificationId, UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        Notification notification = notificationRepository
                .findByIdAndRecipientIdAndTenantId(notificationId, residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        notification = notificationRepository.save(notification);

        return notificationMapper.toResponse(notification);
    }

    public void markAllAsRead(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        int updated = notificationRepository.markAllAsReadByRecipientIdAndTenantId(residentId, tenantId);
        log.info("Marked {} notifications as read for resident {}", updated, residentId);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        long count = notificationRepository.countByRecipientIdAndStatusAndTenantId(
                residentId, NotificationStatus.DELIVERED, tenantId);
        return UnreadCountResponse.builder().count(count).build();
    }

    private List<UUID> findResidentIdsByEstateId(UUID estateId, UUID tenantId) {
        // Get all units in the estate
        var units = unitRepository.findByEstateIdAndTenantId(estateId, tenantId);
        Set<UUID> residentIds = new HashSet<>();

        for (var unit : units) {
            // Find active tenancies for each unit
            var tenancies = tenancyRepository.findActiveByUnitIdAndTenantId(unit.getId(), tenantId);
            for (var tenancy : tenancies) {
                residentIds.add(tenancy.getResidentId());
            }
        }

        return new ArrayList<>(residentIds);
    }

    private PagedResponse<NotificationResponse> toPagedResponse(Page<Notification> page) {
        return PagedResponse.<NotificationResponse>builder()
                .content(page.getContent().stream().map(notificationMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
