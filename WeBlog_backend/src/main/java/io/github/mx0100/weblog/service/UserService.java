package io.github.mx0100.weblog.service;

import io.github.mx0100.weblog.common.ResponseCode;
import io.github.mx0100.weblog.dto.request.PasswordChangeRequest;
import io.github.mx0100.weblog.dto.request.UserRegisterRequest;
import io.github.mx0100.weblog.dto.request.UserUpdateRequest;
import io.github.mx0100.weblog.dto.response.LoginResponse;
import io.github.mx0100.weblog.dto.response.UserResponse;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.repository.UserRepository;
import io.github.mx0100.weblog.utils.BeanUtils;
import io.github.mx0100.weblog.utils.JwtUtils;
import io.github.mx0100.weblog.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User service
 * Handle user-related business logic with relationship status
 * 
 * @author mx0100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserRelationshipService userRelationshipService;
    
    /**
     * Register new user
     * 
     * @param request register request
     * @return user response
     * @throws RuntimeException if username already exists
     */
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        log.info("Registering user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new RuntimeException(ResponseCode.USERNAME_EXISTS.getMessage());
        }
        // Create new user entity;
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtils.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setGender(request.getGender());
        
        // Set profile image and hobby if provided
        if (request.getProfileimg() != null) {
            user.setProfileimg(request.getProfileimg());
        }
        if (request.getHobby() != null) {
            user.setHobby(request.getHobby());
        }
        
        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());
        
        return BeanUtils.toUserResponse(savedUser, userRelationshipService.getRelationshipStatus(savedUser.getUserId()));
    }
    
    /**
     * User login
     * 
     * @param username username
     * @param password password
     * @return login response with token
     * @throws RuntimeException if user not found or password incorrect
     */
    public LoginResponse login(String username, String password) {
        log.info("User login attempt with username: {}", username);
        
        // Find user by username
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        User user = userOpt.get();
        
        // Verify password
        if (!PasswordUtils.matches(password, user.getPassword())) {
            log.warn("Invalid password for user: {}", username);
            throw new RuntimeException(ResponseCode.INVALID_PASSWORD.getMessage());
        }
        
        // Generate JWT token
        String token = jwtUtils.generateToken(user.getUserId(), user.getUsername());
        log.info("User logged in successfully: {}", username);
        
        return BeanUtils.toLoginResponse(token, user, userRelationshipService.getRelationshipStatus(user.getUserId()));
    }
    
    /**
     * Get user by ID
     * 
     * @param userId user ID
     * @return user response
     * @throws RuntimeException if user not found
     */
    public UserResponse getUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found with ID: {}", userId);
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        return BeanUtils.toUserResponse(userOpt.get(), userRelationshipService.getRelationshipStatus(userId));
    }
    
    /**
     * Update user information
     * 
     * @param userId user ID
     * @param request update request
     * @param currentUserId current logged-in user ID
     * @return updated user response
     * @throws RuntimeException if user not found or permission denied
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request, Long currentUserId) {
        if (userId == null || currentUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Check permission - user can only update their own profile
        if (!userId.equals(currentUserId)) {
            log.warn("Permission denied: user {} trying to update user {}", currentUserId, userId);
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Find user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found with ID: {}", userId);
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        User user = userOpt.get();
        
        // Update fields if provided
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getProfileimg() != null) {
            user.setProfileimg(request.getProfileimg());
        }
        if (request.getHobby() != null) {
            user.setHobby(request.getHobby());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        
        return BeanUtils.toUserResponse(updatedUser, userRelationshipService.getRelationshipStatus(userId));
    }
    
    /**
     * Change user password
     * 
     * @param userId user ID
     * @param request password change request
     * @param currentUserId current logged-in user ID
     * @throws RuntimeException if user not found, permission denied, or old password incorrect
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request, Long currentUserId) {
        if (userId == null || currentUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Check permission - user can only change their own password
        if (!userId.equals(currentUserId)) {
            log.warn("Permission denied: user {} trying to change password for user {}", currentUserId, userId);
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Find user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found with ID: {}", userId);
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        User user = userOpt.get();
        
        // Verify old password
        if (!PasswordUtils.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Invalid old password for user: {}", userId);
            throw new RuntimeException(ResponseCode.INVALID_PASSWORD.getMessage());
        }
        
        // Update password
        user.setPassword(PasswordUtils.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }
    
    /**
     * Find user entity by ID (internal use)
     * 
     * @param userId user ID
     * @return user entity
     * @throws RuntimeException if user not found
     */
    public User findUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        return userOpt.get();
    }
    
    /**
     * Find user entity by username
     * 
     * @param username username
     * @return user entity
     * @throws RuntimeException if user not found
     */
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException(ResponseCode.USER_NOT_FOUND.getMessage());
        }
        
        return userOpt.get();
    }
} 