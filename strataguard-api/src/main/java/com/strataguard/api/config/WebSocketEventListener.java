package com.strataguard.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketEventListener {

    private final Map<String, Set<String>> connectedUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null && sessionId != null) {
            connectedUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            log.info("WebSocket connected: user={}, session={}", userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null && sessionId != null) {
            Set<String> sessions = connectedUsers.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    connectedUsers.remove(userId);
                }
            }
            log.info("WebSocket disconnected: user={}, session={}", userId, sessionId);
        }
    }

    public boolean isUserOnline(String userId) {
        Set<String> sessions = connectedUsers.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
