package io.github.mx0100.weblog.controller;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.dto.request.PostCreateRequest;
import io.github.mx0100.weblog.dto.request.PostUpdateRequest;
import io.github.mx0100.weblog.dto.response.PageResponse;
import io.github.mx0100.weblog.dto.response.PostResponse;
import io.github.mx0100.weblog.security.UserPrincipal;
import io.github.mx0100.weblog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Post controller
 * Handle post-related operations
 * 
 * @author mx0100
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    
    /**
     * Create new post
     * 
     * @param request create post request
     * @param userPrincipal current authenticated user
     * @return post response
     */
    @PostMapping
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostCreateRequest request,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Create post request by user: {}", userPrincipal.getUserId());
        
        PostResponse postResponse = postService.createPost(request, userPrincipal.getUserId());
        return ApiResponse.success(postResponse);
    }
    
    /**
     * Get posts with pagination (filtered by user relationship)
     * 
     * @param page page number (0-based)
     * @param size page size
     * @param userPrincipal current authenticated user
     * @return page response of posts
     */
    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> getPosts(@RequestParam(defaultValue = "0") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Get posts request by user: {} - page: {}, size: {}", userPrincipal.getUserId(), page, size);
        
        PageResponse<PostResponse> pageResponse = postService.getPosts(userPrincipal.getUserId(), page, size);
        return ApiResponse.success(pageResponse);
    }
    
    /**
     * Get post by ID
     * 
     * @param postId post ID
     * @param userPrincipal current authenticated user
     * @return post response
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long postId,
                                                 @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Get post request for ID: {} by user: {}", postId, userPrincipal.getUserId());
        
        PostResponse postResponse = postService.getPostById(postId, userPrincipal.getUserId());
        return ApiResponse.success(postResponse);
    }
    
    /**
     * Update post
     * 
     * @param postId post ID
     * @param request update request
     * @param userPrincipal current authenticated user
     * @return updated post response
     */
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(@PathVariable Long postId,
                                                @Valid @RequestBody PostUpdateRequest request,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Update post request for ID: {} by user: {}", postId, userPrincipal.getUserId());
        
        PostResponse postResponse = postService.updatePost(postId, request, userPrincipal.getUserId());
        return ApiResponse.success(postResponse);
    }
    
    /**
     * Delete post
     * 
     * @param postId post ID
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Delete post request for ID: {} by user: {}", postId, userPrincipal.getUserId());
        
        postService.deletePost(postId, userPrincipal.getUserId());
        return ApiResponse.success();
    }
} 