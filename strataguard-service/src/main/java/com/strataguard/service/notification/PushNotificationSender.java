package com.strataguard.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationSender implements NotificationSender {

    private final NotificationConfig notificationConfig;
    private boolean fcmInitialized = false;

    @PostConstruct
    public void init() {
        String credentialsPath = notificationConfig.getFcm().getCredentialsPath();
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM credentials path not configured. Push notifications will be disabled.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                        .build();
                FirebaseApp.initializeApp(options);
                fcmInitialized = true;
                log.info("Firebase initialized successfully for push notifications");
            } else {
                fcmInitialized = true;
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Firebase: {}. Push notifications will be disabled.", e.getMessage());
        }
    }

    @Override
    public void send(Notification notification) {
        if (!fcmInitialized) {
            throw new RuntimeException("Channel not configured: FCM is not initialized");
        }

        // FCM requires a device token. For now, we use the recipientId as a topic.
        // In a real implementation, you'd look up the device token from a device registry.
        String topic = "user-" + notification.getRecipientId().toString();

        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .putData("title", notification.getTitle())
                    .putData("body", notification.getBody())
                    .putData("type", notification.getType().name())
                    .putData("notificationId", notification.getId().toString())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getBody())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent for notification {}: {}", notification.getId(), response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send push notification: " + e.getMessage(), e);
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }
}
