package io.github.mx0100.weblog.security;

import io.github.mx0100.weblog.config.JwtConfig;
import io.github.mx0100.weblog.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT authentication filter
 * Extract and validate JWT token from request header
 * 
 * @author mx0100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    private final JwtConfig jwtConfig;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                String username = jwtUtils.getUsernameFromToken(token);
                
                if (userId != null && username != null) {
                    // Create authentication object
                    UserPrincipal userPrincipal = new UserPrincipal(userId, username);
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Set Authentication to security context for '{}', uri: {}", username, request.getRequestURI());
                } else {
                    log.warn("Cannot set authentication with invalid token");
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from request header
     * 
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.getHeaderName());
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtConfig.getTokenPrefix())) {
            return bearerToken.substring(jwtConfig.getTokenPrefix().length());
        }
        
        return null;
    }
} 