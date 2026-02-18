package com.strataguard.service.notification;

import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.infrastructure.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final ResidentRepository residentRepository;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.from:noreply@strataguard.com}")
    private String fromAddress;

    @Override
    public void send(Notification notification) {
        if (mailHost == null || mailHost.isBlank()) {
            throw new RuntimeException("Channel not configured: Email mail host is not set");
        }

        String recipientEmail = residentRepository.findByIdAndTenantId(
                        notification.getRecipientId(), notification.getTenantId())
                .map(r -> r.getEmail())
                .orElseThrow(() -> new RuntimeException("Recipient email not found"));

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new RuntimeException("Recipient does not have an email address");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject(notification.getTitle());
        message.setText(notification.getBody());

        mailSender.send(message);
        log.info("Email notification sent to {} for notification {}", recipientEmail, notification.getId());
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }
}
