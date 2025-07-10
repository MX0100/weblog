package io.github.mx0100.weblog.service;

import io.github.mx0100.weblog.common.ResponseCode;
import io.github.mx0100.weblog.dto.RichContent;
import io.github.mx0100.weblog.dto.request.CommentCreateRequest;
import io.github.mx0100.weblog.dto.response.CommentResponse;
import io.github.mx0100.weblog.dto.response.PageResponse;
import io.github.mx0100.weblog.entity.Comment;
import io.github.mx0100.weblog.entity.Post;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.repository.CommentRepository;
import io.github.mx0100.weblog.repository.UserRepository;
import io.github.mx0100.weblog.utils.BeanUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comment service
 * Handle comment-related business logic with couple blog access control
 * Support for rich text content
 * 
 * @author mx0100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostService postService;
    private final UserRelationshipService userRelationshipService;
    private final NotificationService notificationService;
    
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * Create new comment with rich text support
     * 
     * @param postId post ID
     * @param request create comment request
     * @param userId comment author user ID
     * @return comment response
     * @throws RuntimeException if post not found or access denied
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, Long userId) {
        if (postId == null || userId == null) {
            throw new IllegalArgumentException("Post ID and user ID cannot be null");
        }
        
        // Validate request content
        if (!request.hasValidContent()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        
        log.info("Creating comment for post {} by user {}", postId, userId);
        
        // Verify post exists and user has access to it
        Post post = postService.findPostById(postId);
        
        // Check if user can comment on this post (only couples can comment on each other's posts)
        if (!userRelationshipService.canUserAccessContent(userId, post.getUserId())) {
            log.warn("Access denied: user {} trying to comment on post {} (owner: {})", 
                    userId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Verify author exists
        User author = findUserById(userId);
        
        // Create new comment entity
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        
        // Set content based on request type (rich text or legacy plain text)
        RichContent effectiveContent = request.getEffectiveContent();
        if (effectiveContent != null) {
            comment.setRichContent(new RichContent(
                effectiveContent.getType(),
                effectiveContent.getVersion(),
                effectiveContent.getDelta(),
                effectiveContent.getPlainText()
            ));
            log.debug("Created comment with {} content", effectiveContent.getType());
        } else {
            // This should not happen due to validation, but provide fallback
            comment.setPlainTextContent("");
            log.warn("Created comment with empty content for user {}", userId);
        }
        
        // Save comment to database
        Comment savedComment = commentRepository.save(comment);
        
        log.info("Comment created successfully with ID: {} (content type: {})", 
                savedComment.getCommentId(), savedComment.hasRichTextContent() ? "rich_text" : "plain_text");
        
        // Send notification to post author about new comment
        notificationService.sendNewCommentNotification(savedComment, post);
        
        return BeanUtils.toCommentResponse(savedComment, author);
    }
    
    /**
     * Get comments by post ID with pagination
     * 
     * @param postId post ID
     * @param currentUserId current user ID (for access control)
     * @param page page number (0-based)
     * @param size page size
     * @return page response of comments
     */
    public PageResponse<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId, 
                                                           Integer page, Integer size) {
        if (postId == null || currentUserId == null) {
            throw new IllegalArgumentException("Post ID and current user ID cannot be null");
        }
        
        // Verify post exists and user has access to it
        Post post = postService.findPostById(postId);
        
        // Check if user can access this post and its comments
        if (!userRelationshipService.canUserAccessContent(currentUserId, post.getUserId())) {
            log.warn("Access denied: user {} trying to view comments of post {} (owner: {})", 
                    currentUserId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Set default values
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        log.info("Getting comments for post {} by user {} - page: {}, size: {}", 
                postId, currentUserId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        
        if (commentPage.isEmpty()) {
            return PageResponse.of(commentPage.map(comment -> null));
        }
        
        // Get all unique user IDs for author loading
        List<Long> userIds = commentPage.getContent().stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // Batch load users to avoid N+1 problem
        Map<Long, User> userMap = loadUsersMap(userIds);
        
        // Convert to response DTOs
        Page<CommentResponse> responsePage = commentPage.map(comment -> 
                BeanUtils.toCommentResponse(comment, userMap.get(comment.getUserId())));
        
        return PageResponse.of(responsePage);
    }
    
    /**
     * Get comments by post ID (non-paginated)
     * 
     * @param postId post ID
     * @param currentUserId current user ID (for access control)
     * @return list of comment responses
     */
    public List<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId) {
        if (postId == null || currentUserId == null) {
            throw new IllegalArgumentException("Post ID and current user ID cannot be null");
        }
        
        // Verify post exists and user has access to it
        Post post = postService.findPostById(postId);
        
        // Check if user can access this post and its comments
        if (!userRelationshipService.canUserAccessContent(currentUserId, post.getUserId())) {
            log.warn("Access denied: user {} trying to view comments of post {} (owner: {})", 
                    currentUserId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        log.info("Getting comments for post {} by user {}", postId, currentUserId);
        
        // Get comments for the post
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        
        if (comments.isEmpty()) {
            return List.of();
        }
        
        // Get all unique user IDs for author loading
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // Batch load users to avoid N+1 problem
        Map<Long, User> userMap = loadUsersMap(userIds);

        // Convert to response DTOs
        return comments.stream()
                .map(comment -> BeanUtils.toCommentResponse(comment, userMap.get(comment.getUserId())))
                .collect(Collectors.toList());
    }
    
    /**
     * Get comments by comment IDs (batch operation)
     * 
     * @param commentIds list of comment IDs
     * @param currentUserId current user ID (for access control)
     * @return list of comment responses
     */
    public List<CommentResponse> getCommentsByIds(List<Long> commentIds, Long currentUserId) {
        if (commentIds == null || commentIds.isEmpty() || currentUserId == null) {
            return List.of();
        }
        
        log.info("Getting comments by IDs for user {}: {}", currentUserId, commentIds);
        
        // Get comments by IDs
        List<Comment> comments = commentRepository.findByCommentIdIn(commentIds);
        
        if (comments.isEmpty()) {
            return List.of();
        }
        
        // Filter comments user can access (only comments on posts they can access)
        List<Comment> accessibleComments = comments.stream()
                .filter(comment -> {
                    try {
                        Post post = postService.findPostById(comment.getPostId());
                        return userRelationshipService.canUserAccessContent(currentUserId, post.getUserId());
                    } catch (Exception e) {
                        log.warn("Failed to check access for comment {}", comment.getCommentId());
                        return false;
                    }
                })
                .collect(Collectors.toList());
        
        if (accessibleComments.isEmpty()) {
            return List.of();
        }
        
        // Get all unique user IDs for author loading
        List<Long> userIds = accessibleComments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // Batch load users to avoid N+1 problem
        Map<Long, User> userMap = loadUsersMap(userIds);
        
        // Convert to response DTOs and maintain order based on input commentIds
        Map<Long, CommentResponse> commentMap = accessibleComments.stream()
                .collect(Collectors.toMap(
                        Comment::getCommentId,
                        comment -> BeanUtils.toCommentResponse(comment, userMap.get(comment.getUserId()))
                ));
        
        return commentIds.stream()
                .map(commentMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete comment
     * 
     * @param commentId comment ID
     * @param currentUserId current logged-in user ID
     * @throws RuntimeException if comment not found or permission denied
     */
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        if (commentId == null || currentUserId == null) {
            throw new IllegalArgumentException("Comment ID and current user ID cannot be null");
        }
        
        log.info("Deleting comment {} by user {}", commentId, currentUserId);
        
        // Find comment
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            log.warn("Comment not found with ID: {}", commentId);
            throw new RuntimeException(ResponseCode.COMMENT_NOT_FOUND.getMessage());
        }
        
        Comment comment = commentOpt.get();
        
        // Check permission - only comment author can delete their comment
        if (!comment.getUserId().equals(currentUserId)) {
            log.warn("Permission denied: user {} trying to delete comment {} (author: {})", 
                    currentUserId, commentId, comment.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Get post for notification before deleting comment
        Post post = postService.findPostById(comment.getPostId());
        
        // Send notification to post author about comment deletion
        notificationService.sendCommentDeletedNotification(comment, post);
        
        // Delete comment
        commentRepository.delete(comment);
        
        log.info("Comment deleted successfully: {}", commentId);
    }
    
    /**
     * Find comment entity by ID (internal method without access control)
     * 
     * @param commentId comment ID
     * @return comment entity
     * @throws RuntimeException if comment not found
     */
    public Comment findCommentById(Long commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("Comment ID cannot be null");
        }
        
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException(ResponseCode.COMMENT_NOT_FOUND.getMessage());
        }
        
        return commentOpt.get();
    }
    
    /**
     * Find user by ID (internal method)
     * 
     * @param userId user ID
     * @return user entity
     * @throws RuntimeException if user not found
     */
    private User findUserById(Long userId) {
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
     * Load users map for batch loading
     * 
     * @param userIds list of user IDs
     * @return map of user ID to User entity
     */
    private Map<Long, User> loadUsersMap(List<Long> userIds) {
        Map<Long, User> userMap = new HashMap<>();
        for (Long userId : userIds) {
            try {
                User user = findUserById(userId);
                userMap.put(userId, user);
            } catch (Exception e) {
                log.warn("Failed to load comment author with ID: {}", userId);
            }
        }
        return userMap;
    }
} 