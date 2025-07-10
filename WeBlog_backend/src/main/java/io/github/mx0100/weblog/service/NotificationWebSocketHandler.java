package io.github.mx0100.weblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mx0100.weblog.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for notifications
 * 
 * @author mx0100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    
    // Store mapping from user ID to WebSocket session
    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("✅ WebSocket connection established successfully, user ID: {}, session ID: {}", userId, session.getId());
            log.info("📊 Current online users: {}", userSessions.size());
            log.info("🔗 User {} is now ONLINE", userId);
            
            // 发送pending通知给刚上线的用户
            sendPendingNotifications(userId);
        } else {
            log.warn("❌ WebSocket connection failed, user ID not found");
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Currently mainly server-to-client push messages, fewer client messages
        // Can handle client messages here, such as heartbeat packets
        log.debug("Received WebSocket message: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId != null) {
            userSessions.remove(userId);
            log.info("📱 WebSocket connection closed, user ID: {}, session ID: {}", userId, session.getId());
            log.info("📊 Current online users: {}", userSessions.size());
            log.info("💤 User {} is now OFFLINE", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket transport error, user ID: {}, session ID: {}", userId, session.getId(), exception);
        
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
    
    /**
     * Send notification message to specified user
     * 
     * @param userId user ID
     * @param message notification message
     */
    public void sendNotificationToUser(Long userId, NotificationMessage message) {
        WebSocketSession session = userSessions.get(userId);
        
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.info("📤 Sent WebSocket notification to user {}: {}", userId, jsonMessage);
            } catch (IOException e) {
                log.error("Failed to send WebSocket message, user ID: {}", userId, e);
                // Remove invalid session if sending fails
                userSessions.remove(userId);
            }
        } else {
            log.debug("User {} is not online, unable to send WebSocket notification", userId);
        }
    }
    
    /**
     * Get online user count
     * 
     * @return online user count
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
    
    /**
     * Check if user is online
     * 
     * @param userId user ID
     * @return whether online
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        boolean isOnline = session != null && session.isOpen();
        log.debug("🔍 Checking online status for user {}: {}", userId, isOnline ? "ONLINE" : "OFFLINE");
        return isOnline;
    }
    
    /**
     * Send pending notifications to user who just came online
     * 
     * @param userId user ID
     */
    private void sendPendingNotifications(Long userId) {
        // TODO: Implement persistent notification storage
        // For now, just log that user is online and ready to receive notifications
        log.info("📬 User {} is online and ready to receive notifications", userId);
    }
} 