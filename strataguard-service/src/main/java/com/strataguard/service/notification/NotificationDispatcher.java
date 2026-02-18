package com.strataguard.service.notification;

import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationStatus;
import com.strataguard.infrastructure.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannel, NotificationSender> senderMap;
    private final NotificationConfig notificationConfig;

    public NotificationDispatcher(NotificationRepository notificationRepository,
                                   List<NotificationSender> senders,
                                   NotificationConfig notificationConfig) {
        this.notificationRepository = notificationRepository;
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, Function.identity()));
        this.notificationConfig = notificationConfig;
    }

    @Async("notificationExecutor")
    public void dispatch(Notification notification) {
        if (notification.getChannel() == NotificationChannel.IN_APP) {
            // IN_APP notifications are already persisted; just mark as DELIVERED
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
            return;
        }

        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            log.warn("No sender registered for channel: {}", notification.getChannel());
            markFailed(notification, "No sender registered for channel: " + notification.getChannel());
            return;
        }

        try {
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);

            sender.send(notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
            log.debug("Notification {} dispatched via {}", notification.getId(), notification.getChannel());
        } catch (Exception e) {
            log.error("Failed to dispatch notification {} via {}: {}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            handleFailure(notification, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${notification.retry.interval-seconds:60}000")
    public void retryPendingNotifications() {
        int maxRetries = notificationConfig.getRetry().getMaxRetries();
        List<Notification> pending = notificationRepository.findPendingForRetry(
                NotificationStatus.PENDING, maxRetries);

        if (!pending.isEmpty()) {
            log.info("Retrying {} pending notifications", pending.size());
            for (Notification notification : pending) {
                dispatch(notification);
            }
        }
    }

    private void handleFailure(Notification notification, String error) {
        int maxRetries = notificationConfig.getRetry().getMaxRetries();
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setLastError(error);

        if (notification.getRetryCount() >= maxRetries) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification {} permanently failed after {} retries: {}",
                    notification.getId(), maxRetries, error);
        } else {
            notification.setStatus(NotificationStatus.PENDING);
            log.info("Notification {} will be retried (attempt {}/{})",
                    notification.getId(), notification.getRetryCount(), maxRetries);
        }
        notificationRepository.save(notification);
    }

    private void markFailed(Notification notification, String error) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setLastError(error);
        notificationRepository.save(notification);
    }
}
