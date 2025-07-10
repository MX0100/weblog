package io.github.mx0100.weblog.security;

import io.github.mx0100.weblog.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket authentication interceptor
 * 
 * @author mx0100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        log.info("🔍 WebSocket connection attempt from: {}", request.getRemoteAddress());
        log.info("🔍 Request URI: {}", request.getURI());
        log.info("🔍 Request query: {}", request.getURI().getQuery());
        
        // 从查询参数中获取token
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = extractTokenFromQuery(query);
            log.info("🔍 Extracted token from query: {}", token != null ? "TOKEN_FOUND" : "TOKEN_NULL");
            
            if (token != null) {
                boolean isValid = jwtUtils.validateToken(token);
                log.info("🔍 Token validation result: {}", isValid);
                
                if (isValid) {
                    // 提取用户ID并存储在session attributes中
                    Long userId = jwtUtils.getUserIdFromToken(token);
                    attributes.put("userId", userId);
                    
                    log.info("✅ WebSocket connection authentication successful, user ID: {}", userId);
                    return true;
                } else {
                    log.warn("❌ WebSocket authentication failed: invalid token");
                    return false;
                }
            } else {
                log.warn("❌ WebSocket authentication failed: token extraction failed");
                return false;
            }
        }
        
        // 从Header中获取token (备用方案)
        String authHeader = request.getHeaders().getFirst("Authorization");
        log.info("🔍 Authorization header: {}", authHeader != null ? "HEADER_FOUND" : "HEADER_NULL");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                attributes.put("userId", userId);
                
                log.info("✅ WebSocket connection authentication successful (Header), user ID: {}", userId);
                return true;
            } else {
                log.warn("❌ WebSocket authentication failed: invalid token in header");
                return false;
            }
        }
        
        log.warn("❌ WebSocket connection authentication failed: no valid token found");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理
    }
    
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                return param.substring(6); // "token=".length()
            }
        }
        return null;
    }
} 