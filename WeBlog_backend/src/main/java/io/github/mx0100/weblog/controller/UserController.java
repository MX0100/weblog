package io.github.mx0100.weblog.controller;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.common.ResponseCode;
import io.github.mx0100.weblog.dto.request.PasswordChangeRequest;
import io.github.mx0100.weblog.dto.request.UserUpdateRequest;
import io.github.mx0100.weblog.dto.response.UserResponse;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.security.UserPrincipal;
import io.github.mx0100.weblog.service.UserService;
import io.github.mx0100.weblog.service.NotificationWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.mx0100.weblog.utils.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * User controller
 * Handle user-related operations
 * 
 * @author mx0100
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final NotificationWebSocketHandler webSocketHandler;
    
    /**
     * Get user by ID
     * 
     * @param userId user ID
     * @return user response
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("Get user request for ID: {}", userId);
        UserResponse userResponse = userService.getUserById(userId);
        System.out.println("Get user request for ID: " + userId);
        return ApiResponse.success(userResponse);
    }
    /**
     * Update user information
     * 
     * @param userId user ID
     * @param request update request
     * @param userPrincipal current authenticated user
     * @return updated user response
     */
    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userId,
                                                @Valid @RequestBody UserUpdateRequest request,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Update user request for ID: {} by user: {}", userId, userPrincipal.getUserId());
        
        UserResponse userResponse = userService.updateUser(userId, request, userPrincipal.getUserId());
        return ApiResponse.success(userResponse);
    }
    
    /**
     * Change user password
     * 
     * @param userId user ID
     * @param request password change request
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(@PathVariable Long userId,
                                            @Valid @RequestBody PasswordChangeRequest request,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Change password request for ID: {} by user: {}", userId, userPrincipal.getUserId());
        
        userService.changePassword(userId, request, userPrincipal.getUserId());
        return ApiResponse.success();
    }

    /**
     * Search user by username
     * 
     * @param username username to search
     * @return user information
     */
    @GetMapping("/search")
    public ApiResponse<UserResponse> searchUserByUsername(@RequestParam String username) {
        log.info("Search user by username: {}", username);
        
        User user = userService.findByUsername(username);
        UserResponse userResponse = BeanUtils.toUserResponse(user);
        
        return ApiResponse.success(userResponse);
    }

    /**
     * 获取在线用户调试信息
     */
    @GetMapping("/debug/online")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUsersDebugInfo(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long currentUserId = userPrincipal.getUserId();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("currentUserId", currentUserId);
            debugInfo.put("currentUserOnline", webSocketHandler.isUserOnline(currentUserId));
            debugInfo.put("totalOnlineUsers", webSocketHandler.getOnlineUserCount());
            
            log.info("🔍 Debug info requested by user {}: {}", currentUserId, debugInfo);
            
            return ResponseEntity.ok(ApiResponse.success(debugInfo));
            
        } catch (Exception e) {
            log.error("获取在线用户调试信息失败", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error(ResponseCode.INTERNAL_ERROR, "获取调试信息失败"));
        }
    }
} 