package com.strataguard.service.notification;

import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.infrastructure.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationSender implements NotificationSender {

    private final NotificationConfig notificationConfig;
    private final WebClient termiiWebClient;
    private final ResidentRepository residentRepository;

    @Override
    public void send(Notification notification) {
        String apiKey = notificationConfig.getTermii().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Channel not configured: Termii API key is not set for WhatsApp");
        }

        String phone = residentRepository.findByIdAndTenantId(
                        notification.getRecipientId(), notification.getTenantId())
                .map(r -> r.getPhone())
                .orElseThrow(() -> new RuntimeException("Recipient phone not found"));

        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Recipient does not have a phone number");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("api_key", apiKey);
        request.put("to", phone);
        request.put("from", notificationConfig.getTermii().getSenderId());
        request.put("sms", notification.getBody());
        request.put("type", "plain");
        request.put("channel", "whatsapp");

        String response = termiiWebClient.post()
                .uri("/sms/send")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("WhatsApp notification sent to {} for notification {}: {}", phone, notification.getId(), response);
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WHATSAPP;
    }
}
