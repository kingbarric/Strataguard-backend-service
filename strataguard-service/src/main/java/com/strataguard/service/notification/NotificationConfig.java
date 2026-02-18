package com.strataguard.service.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "notification")
@Getter
@Setter
public class NotificationConfig {

    private Termii termii = new Termii();
    private Fcm fcm = new Fcm();
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Termii {
        private String apiKey;
        private String senderId;
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class Fcm {
        private String credentialsPath;
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxRetries = 3;
        private int intervalSeconds = 60;
    }

    @Bean
    public WebClient termiiWebClient() {
        String baseUrl = termii.getBaseUrl() != null ? termii.getBaseUrl() : "https://api.ng.termii.com/api";
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
