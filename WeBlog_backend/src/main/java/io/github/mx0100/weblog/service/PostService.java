package io.github.mx0100.weblog.service;

import io.github.mx0100.weblog.common.ResponseCode;
import io.github.mx0100.weblog.dto.RichContent;
import io.github.mx0100.weblog.dto.request.PostCreateRequest;
import io.github.mx0100.weblog.dto.request.PostUpdateRequest;
import io.github.mx0100.weblog.dto.response.PageResponse;
import io.github.mx0100.weblog.dto.response.PostResponse;
import io.github.mx0100.weblog.entity.Post;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.repository.CommentRepository;
import io.github.mx0100.weblog.repository.PostRepository;
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
 * Post service
 * Handle post-related business logic with couple blog access control
 * Support for rich text content
 * 
 * @author mx0100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserRelationshipService userRelationshipService;
    private final NotificationService notificationService;
    
    private static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * Create new post with rich text support
     * 
     * @param request create post request
     * @param userId author user ID
     * @return post response
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Validate request content
        if (!request.hasValidContent()) {
            throw new IllegalArgumentException("Post content cannot be empty");
        }
        
        log.info("Creating post for user: {}", userId);
        
        // Verify user exists
        User author = findUserById(userId);
        
        // Create new post entity
        Post post = new Post();
        post.setUserId(userId);
        
        // Set content based on request type (rich text or legacy plain text)
        RichContent effectiveContent = request.getEffectiveContent();
        if (effectiveContent != null) {
            post.setRichContent(new RichContent(
                effectiveContent.getType(),
                effectiveContent.getVersion(),
                effectiveContent.getDelta(),
                effectiveContent.getPlainText()
            ));
            log.debug("Created post with {} content", effectiveContent.getType());
        } else {
            // This should not happen due to validation, but provide fallback
            post.setPlainTextContent("");
            log.warn("Created post with empty content for user {}", userId);
        }
        
        // Save post to database
        Post savedPost = postRepository.save(post);
        log.info("Post created successfully with ID: {} (content type: {})", 
                savedPost.getPostId(), savedPost.hasRichTextContent() ? "rich_text" : "plain_text");
        
        // New post has no comments
        List<Long> commentIds = new ArrayList<>();
        int commentsCount = 0;

        // Send notification to partner about new post
        notificationService.sendNewPostNotification(savedPost);
        
        return BeanUtils.toPostResponse(savedPost, author, commentIds, commentsCount);
    }
    
    /**
     * Get posts with pagination (filtered by user relationship)
     * 
     * @param currentUserId current user ID (for filtering)
     * @param page page number (0-based)
     * @param size page size (default: 10)
     * @return page response of posts
     */
    public PageResponse<PostResponse> getPosts(Long currentUserId, Integer page, Integer size) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Current user ID cannot be null");
        }
        
        // Set default values
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        log.info("Getting posts for user {} - page: {}, size: {}", currentUserId, page, size);
        
        // Get visible user IDs (self + partner if coupled)
        List<Long> visibleUserIds = getVisibleUserIds(currentUserId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserIdInOrderByCreatedAtDesc(visibleUserIds, pageable);
        
        // Get all unique user IDs for author loading
        List<Long> userIds = postPage.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // Batch load users to avoid N+1 problem
        Map<Long, User> userMap = loadUsersMap(userIds);
        
        // Batch load comment information for all posts
        List<Long> postIds = postPage.getContent().stream()
                .map(Post::getPostId)
                .collect(Collectors.toList());
        
        Map<Long, List<Long>> postCommentIdsMap = loadPostCommentIds(postIds);
        Map<Long, Integer> postCommentsCountMap = loadPostCommentsCount(postIds);
        
        // Convert to response DTOs
        Page<PostResponse> responsePage = postPage.map(post -> 
                BeanUtils.toPostResponse(post, userMap.get(post.getUserId()),
                        postCommentIdsMap.get(post.getPostId()),
                        postCommentsCountMap.get(post.getPostId())));
        
        return PageResponse.of(responsePage);
    }
    
    /**
     * Get post by ID with access control
     * 
     * @param postId post ID
     * @param currentUserId current user ID (for access control)
     * @return post response
     * @throws RuntimeException if post not found or access denied
     */
    public PostResponse getPostById(Long postId, Long currentUserId) {
        if (postId == null || currentUserId == null) {
            throw new IllegalArgumentException("Post ID and current user ID cannot be null");
        }
        
        log.info("Getting post {} for user {}", postId, currentUserId);
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post not found with ID: {}", postId);
            throw new RuntimeException(ResponseCode.POST_NOT_FOUND.getMessage());
        }
        
        Post post = postOpt.get();
        
        // Check access permission
        if (!userRelationshipService.canUserAccessContent(currentUserId, post.getUserId())) {
            log.warn("Access denied: user {} trying to access post {} (owner: {})", 
                    currentUserId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        User author = findUserById(post.getUserId());
        
        // Load comment information for single post
        List<Long> commentIds = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(comment -> comment.getCommentId())
                .collect(Collectors.toList());
        int commentsCount = (int) commentRepository.countByPostId(postId);
        
        return BeanUtils.toPostResponse(post, author, commentIds, commentsCount);
    }
    
    /**
     * Update post with rich text support
     * 
     * @param postId post ID
     * @param request update request
     * @param currentUserId current logged-in user ID
     * @return updated post response
     * @throws RuntimeException if post not found or permission denied
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Long currentUserId) {
        if (postId == null || currentUserId == null) {
            throw new IllegalArgumentException("Post ID and current user ID cannot be null");
        }

        log.info("Updating post {} by user {}", postId, currentUserId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException(ResponseCode.POST_NOT_FOUND.getMessage()));

        // Check ownership
        if (!post.getUserId().equals(currentUserId)) {
            log.warn("Permission denied: user {} trying to update post {} owned by {}",
                    currentUserId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }

        // Validate and update content
        if (request.hasValidContent()) {
            RichContent newContent = request.getRichContent();
            
            // Set the new content on the existing post entity
            post.setRichContent(new RichContent(
                newContent.getType(),
                newContent.getVersion(),
                newContent.getDelta(),
                newContent.getPlainText()
            ));
            
            Post updatedPost = postRepository.save(post);
            User author = findUserById(updatedPost.getUserId());

            // Send notification to partner about post update
            notificationService.sendPostUpdatedNotification(updatedPost);
            
            // Load comment information for updated post
            List<Long> commentIds = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                    .map(comment -> comment.getCommentId())
                    .collect(Collectors.toList());
            int commentsCount = (int) commentRepository.countByPostId(postId);
            
            log.info("Post updated successfully: {} (content type: {})", 
                    postId, updatedPost.hasRichTextContent() ? "rich_text" : "plain_text");
            return BeanUtils.toPostResponse(updatedPost, author, commentIds, commentsCount);
        } else {
            log.warn("Update request for post {} has invalid or empty content. No changes made.", postId);
            // If content is invalid, return the current state of the post without making changes
            User author = findUserById(post.getUserId());
            List<Long> commentIds = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(comment -> comment.getCommentId())
                .collect(Collectors.toList());
            int commentsCount = (int) commentRepository.countByPostId(postId);
            return BeanUtils.toPostResponse(post, author, commentIds, commentsCount);
        }
    }
    
    /**
     * Delete post
     * 
     * @param postId post ID
     * @param currentUserId current logged-in user ID
     * @throws RuntimeException if post not found or permission denied
     */
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        if (postId == null || currentUserId == null) {
            throw new IllegalArgumentException("Post ID and current user ID cannot be null");
        }
        
        log.info("Deleting post {} by user {}", postId, currentUserId);
        
        // Find post
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            log.warn("Post not found with ID: {}", postId);
            throw new RuntimeException(ResponseCode.POST_NOT_FOUND.getMessage());
        }
        
        Post post = postOpt.get();
        
        // Check permission - only author can delete their post
        if (!post.getUserId().equals(currentUserId)) {
            log.warn("Permission denied: user {} trying to delete post {} (author: {})", 
                    currentUserId, postId, post.getUserId());
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Send notification to partner before deleting
        notificationService.sendPostDeletedNotification(post);
        
        // Delete associated comments first
        commentRepository.deleteByPostId(postId);
        
        // Delete post
        postRepository.delete(post);
        
        log.info("Post deleted successfully: {}", postId);
    }
    
    /**
     * Get posts by user ID with pagination (for profile view)
     * 
     * @param userId target user ID
     * @param currentUserId current user ID (for access control)
     * @param page page number (0-based)
     * @param size page size
     * @return page response of posts
     */
    public PageResponse<PostResponse> getPostsByUserId(Long userId, Long currentUserId, 
                                                      Integer page, Integer size) {
        if (userId == null || currentUserId == null) {
            throw new IllegalArgumentException("User ID and current user ID cannot be null");
        }
        
        // Check access permission
        if (!userRelationshipService.canUserAccessContent(currentUserId, userId)) {
            log.warn("Access denied: user {} trying to view posts of user {}", currentUserId, userId);
            throw new RuntimeException(ResponseCode.PERMISSION_DENIED.getMessage());
        }
        
        // Set default values
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        
        log.info("Getting posts by user {} for viewer {} - page: {}, size: {}", 
                userId, currentUserId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        User author = findUserById(userId);
        
        // Batch load comment information for all posts
        List<Long> postIds = postPage.getContent().stream()
                .map(Post::getPostId)
                .collect(Collectors.toList());
        
        Map<Long, List<Long>> postCommentIdsMap = loadPostCommentIds(postIds);
        Map<Long, Integer> postCommentsCountMap = loadPostCommentsCount(postIds);
        
        // Convert to response DTOs
        Page<PostResponse> responsePage = postPage.map(post -> 
                BeanUtils.toPostResponse(post, author,
                        postCommentIdsMap.get(post.getPostId()),
                        postCommentsCountMap.get(post.getPostId())));
        
        return PageResponse.of(responsePage);
    }
    
    /**
     * Find post by ID (internal method without access control)
     * 
     * @param postId post ID
     * @return post entity
     * @throws RuntimeException if post not found
     */
    public Post findPostById(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("Post ID cannot be null");
        }
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new RuntimeException(ResponseCode.POST_NOT_FOUND.getMessage());
        }
        
        return postOpt.get();
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
     * Get visible user IDs for the current user (self + partner if coupled)
     * 
     * @param currentUserId current user ID
     * @return list of visible user IDs
     */
    private List<Long> getVisibleUserIds(Long currentUserId) {
        List<Long> visibleUserIds = new ArrayList<>();
        visibleUserIds.add(currentUserId);
        
        // Add partner if user is coupled
        Optional<Long> partnerId = userRelationshipService.getPartnerUserId(currentUserId);
        partnerId.ifPresent(visibleUserIds::add);
        
        return visibleUserIds;
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
                log.warn("Failed to load user with ID: {}", userId);
            }
        }
        return userMap;
    }
    
    /**
     * Load comment IDs for posts in batch
     * 
     * @param postIds list of post IDs
     * @return map of post ID to list of comment IDs
     */
    private Map<Long, List<Long>> loadPostCommentIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }
        
        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                                .map(comment -> comment.getCommentId())
                                .collect(Collectors.toList())
                ));
    }
    
    /**
     * Load comments count for posts in batch
     * 
     * @param postIds list of post IDs
     * @return map of post ID to comment count
     */
    private Map<Long, Integer> loadPostCommentsCount(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }
        
        return postIds.stream()
                .collect(Collectors.toMap(
                        postId -> postId,
                        postId -> (int) commentRepository.countByPostId(postId)
                ));
    }
} 