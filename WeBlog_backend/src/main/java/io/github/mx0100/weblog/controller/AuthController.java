package io.github.mx0100.weblog.controller;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.dto.request.UserLoginRequest;
import io.github.mx0100.weblog.dto.request.UserRegisterRequest;
import io.github.mx0100.weblog.dto.response.LoginResponse;
import io.github.mx0100.weblog.dto.response.UserResponse;
import io.github.mx0100.weblog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller
 * Handle user registration and login
 * 
 * @author mx0100
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * User registration
     * 
     * @param request register request
     * @return user response
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("User registration request for username: {}", request.getUsername());
        
        UserResponse userResponse = userService.register(request);
        return ApiResponse.success(userResponse);
    }
    
    /**
     * User login
     * 
     * @param request login request
     * @return login response with token
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("User login request for username: {}", request.getUsername());
        
        LoginResponse loginResponse = userService.login(request.getUsername(), request.getPassword());
        return ApiResponse.success(loginResponse);
    }
} 