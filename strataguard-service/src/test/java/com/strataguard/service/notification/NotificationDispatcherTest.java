package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender smsSender;

    private NotificationConfig notificationConfig;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        notificationConfig = new NotificationConfig();
        notificationConfig.setRetry(new NotificationConfig.Retry());
        notificationConfig.getRetry().setMaxRetries(3);
        notificationConfig.getRetry().setIntervalSeconds(60);

        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(smsSender.getChannel()).thenReturn(NotificationChannel.SMS);

        dispatcher = new NotificationDispatcher(
                notificationRepository,
                List.of(emailSender, smsSender),
                notificationConfig);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Notification buildNotification(NotificationChannel channel) {
        Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setTenantId(TENANT_ID);
        notification.setRecipientId(RECIPIENT_ID);
        notification.setChannel(channel);
        notification.setType(NotificationType.PAYMENT_RECEIVED);
        notification.setTitle("Payment Received");
        notification.setBody("Your payment was received.");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        return notification;
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("should mark IN_APP notification as DELIVERED immediately")
        void shouldMarkInAppAsDelivered() {
            Notification notification = buildNotification(NotificationChannel.IN_APP);

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
            assertThat(notification.getSentAt()).isNotNull();

            verify(notificationRepository).save(notification);
            verify(emailSender, never()).send(any());
            verify(smsSender, never()).send(any());
        }

        @Test
        @DisplayName("should dispatch EMAIL notification via email sender")
        void shouldDispatchEmailViaSender() {
            Notification notification = buildNotification(NotificationChannel.EMAIL);

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notification.getSentAt()).isNotNull();

            verify(emailSender).send(notification);
            verify(notificationRepository, times(2)).save(notification); // SENDING, then SENT
        }

        @Test
        @DisplayName("should dispatch SMS notification via SMS sender")
        void shouldDispatchSmsViaSender() {
            Notification notification = buildNotification(NotificationChannel.SMS);

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);

            verify(smsSender).send(notification);
        }

        @Test
        @DisplayName("should mark as FAILED when no sender registered for channel")
        void shouldMarkFailedWhenNoSender() {
            Notification notification = buildNotification(NotificationChannel.PUSH);

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.getLastError()).contains("No sender registered");

            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should handle sender failure and increment retry count")
        void shouldHandleSenderFailureAndRetry() {
            Notification notification = buildNotification(NotificationChannel.EMAIL);
            notification.setRetryCount(0);

            doThrow(new RuntimeException("SMTP connection failed")).when(emailSender).send(notification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getRetryCount()).isEqualTo(1);
            assertThat(notification.getLastError()).isEqualTo("SMTP connection failed");
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING); // Will be retried
        }

        @Test
        @DisplayName("should mark as FAILED when max retries reached")
        void shouldMarkFailedWhenMaxRetriesReached() {
            Notification notification = buildNotification(NotificationChannel.EMAIL);
            notification.setRetryCount(2); // Already retried twice, max is 3

            doThrow(new RuntimeException("SMTP connection failed")).when(emailSender).send(notification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getRetryCount()).isEqualTo(3);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED); // Max retries reached
            assertThat(notification.getLastError()).isEqualTo("SMTP connection failed");
        }

        @Test
        @DisplayName("should keep PENDING status when retries remain")
        void shouldKeepPendingWhenRetriesRemain() {
            Notification notification = buildNotification(NotificationChannel.SMS);
            notification.setRetryCount(1); // 1 retry done, max is 3

            doThrow(new RuntimeException("API timeout")).when(smsSender).send(notification);
            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            dispatcher.dispatch(notification);

            assertThat(notification.getRetryCount()).isEqualTo(2);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("retryPendingNotifications")
    class RetryPendingNotifications {

        @Test
        @DisplayName("should pick up and dispatch pending notifications")
        void shouldRetryPendingNotifications() {
            Notification pending1 = buildNotification(NotificationChannel.EMAIL);
            pending1.setRetryCount(1);
            Notification pending2 = buildNotification(NotificationChannel.SMS);
            pending2.setId(UUID.randomUUID());
            pending2.setRetryCount(0);

            when(notificationRepository.findPendingForRetry(NotificationStatus.PENDING, 3))
                    .thenReturn(List.of(pending1, pending2));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.retryPendingNotifications();

            // Both should be dispatched
            verify(emailSender).send(pending1);
            verify(smsSender).send(pending2);
        }

        @Test
        @DisplayName("should do nothing when no pending notifications")
        void shouldDoNothingWhenNoPending() {
            when(notificationRepository.findPendingForRetry(NotificationStatus.PENDING, 3))
                    .thenReturn(List.of());

            dispatcher.retryPendingNotifications();

            verify(emailSender, never()).send(any());
            verify(smsSender, never()).send(any());
        }
    }
}
