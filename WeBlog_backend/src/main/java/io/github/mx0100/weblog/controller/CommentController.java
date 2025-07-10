package io.github.mx0100.weblog.controller;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.dto.request.CommentBatchRequest;
import io.github.mx0100.weblog.dto.request.CommentCreateRequest;
import io.github.mx0100.weblog.dto.response.CommentResponse;
import io.github.mx0100.weblog.dto.response.PageResponse;
import io.github.mx0100.weblog.security.UserPrincipal;
import io.github.mx0100.weblog.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comment controller
 * Handle comment-related operations
 * 
 * @author mx0100
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * Create new comment for a post
     * 
     * @param postId post ID
     * @param request create comment request
     * @param userPrincipal current authenticated user
     * @return comment response
     */
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(@PathVariable Long postId,
                                                      @Valid @RequestBody CommentCreateRequest request,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Create comment request for post: {} by user: {}", postId, userPrincipal.getUserId());
        
        CommentResponse commentResponse = commentService.createComment(postId, request, userPrincipal.getUserId());
        return ApiResponse.success(commentResponse);
    }
    
    /**
     * Get comments by post ID with pagination
     * 
     * @param postId post ID
     * @param page page number (0-based)
     * @param size page size
     * @param userPrincipal current authenticated user
     * @return page response of comments
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getCommentsByPostId(@PathVariable Long postId,
                                                                         @RequestParam(defaultValue = "0") Integer page,
                                                                         @RequestParam(defaultValue = "20") Integer size,
                                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Get comments request for post: {} by user: {} - page: {}, size: {}", 
                postId, userPrincipal.getUserId(), page, size);
        
        PageResponse<CommentResponse> comments = commentService.getCommentsByPostId(postId, userPrincipal.getUserId(), page, size);
        return ApiResponse.success(comments);
    }
    
    /**
     * Get comments by IDs (batch operation)
     * 
     * @param request batch request with comment IDs
     * @param userPrincipal current authenticated user
     * @return list of comment responses
     */
    @PostMapping("/comments/batch")
    public ApiResponse<List<CommentResponse>> getCommentsByIds(@Valid @RequestBody CommentBatchRequest request,
                                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Get comments batch request for IDs: {} by user: {}", 
                request.getCommentIds(), userPrincipal.getUserId());
        
        List<CommentResponse> comments = commentService.getCommentsByIds(request.getCommentIds(), userPrincipal.getUserId());
        return ApiResponse.success(comments);
    }
    
    /**
     * Delete comment
     * 
     * @param commentId comment ID
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Delete comment request for ID: {} by user: {}", commentId, userPrincipal.getUserId());
        
        commentService.deleteComment(commentId, userPrincipal.getUserId());
        return ApiResponse.success();
    }
} 