package io.github.mx0100.weblog.exception;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.common.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler
 * Handle all exceptions in a centralized way
 * 
 * @author mx0100
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle validation exceptions
     * 
     * @param ex validation exception
     * @return error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        BindingResult bindingResult = ex.getBindingResult();
        
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        return ApiResponse.error(ResponseCode.BAD_REQUEST.getCode(), "Validation failed", errors);
    }
    
    /**
     * Handle illegal argument exceptions
     * 
     * @param ex illegal argument exception
     * @return error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ApiResponse.error(ResponseCode.BAD_REQUEST, ex.getMessage());
    }
    
    /**
     * Handle business logic exceptions
     * 
     * @param ex runtime exception
     * @return error response
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        log.warn("Business logic error: {}", ex.getMessage());
        
        String message = ex.getMessage();
        
        // Determine response code based on message
        if (message.contains("not found")) {
            return ApiResponse.error(ResponseCode.NOT_FOUND, message);
        } else if (message.contains("already exists")) {
            return ApiResponse.error(ResponseCode.CONFLICT, message);
        } else if (message.contains("Permission denied")) {
            return ApiResponse.error(ResponseCode.FORBIDDEN, message);
        } else if (message.contains("Invalid password") || message.contains("Unauthorized")) {
            return ApiResponse.error(ResponseCode.UNAUTHORIZED, message);
        } else {
            return ApiResponse.error(ResponseCode.BAD_REQUEST, message);
        }
    }
    
    /**
     * Handle all other exceptions
     * 
     * @param ex unknown exception
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ApiResponse.error(ResponseCode.INTERNAL_ERROR);
    }
} 