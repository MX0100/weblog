package io.github.mx0100.weblog.service;

import io.github.mx0100.weblog.dto.NotificationMessage;
import io.github.mx0100.weblog.entity.Comment;
import io.github.mx0100.weblog.entity.Post;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.repository.UserRepository;
import io.github.mx0100.weblog.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Notification service for handling user notifications
 * 
 * @author mx0100
 */
@Slf4j
@Service
public class NotificationService {
    
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    
    @Autowired
    @Lazy
    private UserRelationshipService userRelationshipService;
    
    // Manual constructor to inject only non-lazy dependencies
    public NotificationService(UserRepository userRepository, 
                              NotificationWebSocketHandler webSocketHandler) {
        this.userRepository = userRepository;
        this.webSocketHandler = webSocketHandler;
    }
    
    /**
     * Send pair request notification to target user
     * 
     * @param fromUserId requesting user ID
     * @param toUserId target user ID
     */
    public void sendPairRequestNotification(Long fromUserId, Long toUserId) {
        Optional<User> fromUser = userRepository.findById(fromUserId);
        Optional<User> toUser = userRepository.findById(toUserId);
        
        if (fromUser.isPresent() && toUser.isPresent()) {
            String fromUserName = fromUser.get().getNickname() != null ? 
                fromUser.get().getNickname() : fromUser.get().getUsername();
            String toUserName = toUser.get().getNickname() != null ? 
                toUser.get().getNickname() : toUser.get().getUsername();
            
            log.info("📨 Pair request notification: {} sent pair request to {}", fromUserName, toUserName);
            
            // Prepare notification data
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserName", fromUser.get().getUsername());
            data.put("fromUserNickname", fromUserName);
            
            // Send WebSocket notification
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.Type.PAIR_REQUEST)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .message(fromUserName + " sent you a pair request")
                .data(data)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(toUserId, notification);
        }
    }
    
    /**
     * Send pair request accepted notification to requesting user
     * 
     * @param requestUserId original requesting user ID
     * @param acceptUserId user who accepted the request
     */
    public void sendPairRequestAcceptedNotification(Long requestUserId, Long acceptUserId) {
        Optional<User> requestUser = userRepository.findById(requestUserId);
        Optional<User> acceptUser = userRepository.findById(acceptUserId);
        
        if (requestUser.isPresent() && acceptUser.isPresent()) {
            String requestUserName = requestUser.get().getNickname() != null ? 
                requestUser.get().getNickname() : requestUser.get().getUsername();
            String acceptUserName = acceptUser.get().getNickname() != null ? 
                acceptUser.get().getNickname() : acceptUser.get().getUsername();
            
            log.info("✅ Pair request accepted notification: {} accepted {}'s pair request", acceptUserName, requestUserName);
            
            // Prepare notification data
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserName", acceptUser.get().getUsername());
            data.put("fromUserNickname", acceptUserName);
            
            // Send WebSocket notification
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.Type.PAIR_REQUEST_ACCEPTED)
                .fromUserId(acceptUserId)
                .toUserId(requestUserId)
                .message(acceptUserName + " accepted your pair request")
                .data(data)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(requestUserId, notification);
        }
    }
    
    /**
     * Send pair request rejected notification to requesting user
     * 
     * @param requestUserId original requesting user ID
     * @param rejectUserId user who rejected the request
     */
    public void sendPairRequestRejectedNotification(Long requestUserId, Long rejectUserId) {
        Optional<User> requestUser = userRepository.findById(requestUserId);
        Optional<User> rejectUser = userRepository.findById(rejectUserId);
        
        if (requestUser.isPresent() && rejectUser.isPresent()) {
            String requestUserName = requestUser.get().getNickname() != null ? 
                requestUser.get().getNickname() : requestUser.get().getUsername();
            String rejectUserName = rejectUser.get().getNickname() != null ? 
                rejectUser.get().getNickname() : rejectUser.get().getUsername();
            
            log.info("❌ Pair request rejected notification: {} rejected {}'s pair request", rejectUserName, requestUserName);
            
            // Prepare notification data
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserName", rejectUser.get().getUsername());
            data.put("fromUserNickname", rejectUserName);
            
            // Send WebSocket notification
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.Type.PAIR_REQUEST_REJECTED)
                .fromUserId(rejectUserId)
                .toUserId(requestUserId)
                .message(rejectUserName + " rejected your pair request")
                .data(data)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(requestUserId, notification);
        }
    }
    
    /**
     * Send new post notification to partner
     * 
     * @param post the newly created post
     */
    public void sendNewPostNotification(Post post) {
        log.info("🔍 Checking partner for user {} who posted", post.getUserId());
        
        Optional<Long> partnerId = userRelationshipService.getPartnerUserId(post.getUserId());
        
        if (partnerId.isPresent()) {
            log.info("✅ Partner found: user {} has partner {}", post.getUserId(), partnerId.get());
            
            log.info("📤 Attempting to send WebSocket notification to user {}", partnerId.get());
            
            // 添加调试信息
            log.info("🔍 WebSocketHandler status: {}", webSocketHandler != null ? "NOT_NULL" : "NULL");
            if (webSocketHandler != null) {
                log.info("🔍 Partner {} online status: {}", partnerId.get(), webSocketHandler.isUserOnline(partnerId.get()));
                log.info("🔍 Current online users count: {}", webSocketHandler.getOnlineUserCount());
            }
            
            Optional<User> postAuthor = userRepository.findById(post.getUserId());
            
            if (postAuthor.isPresent()) {
                String authorName = postAuthor.get().getNickname() != null ? 
                    postAuthor.get().getNickname() : postAuthor.get().getUsername();
                
                log.info("📝 New post notification: {} published a new post", authorName);
                
                // Prepare notification data
                Map<String, Object> data = new HashMap<>();
                data.put("fromUserName", postAuthor.get().getUsername());
                data.put("fromUserNickname", authorName);
                data.put("postId", post.getPostId());
                data.put("postContent", post.getContent().length() > 50 ? 
                    post.getContent().substring(0, 50) + "..." : post.getContent());
                
                // Send WebSocket notification
                NotificationMessage notification = NotificationMessage.builder()
                    .type(NotificationMessage.Type.NEW_POST)
                    .fromUserId(post.getUserId())
                    .toUserId(partnerId.get())
                    .message(authorName + " published a new post")
                    .data(data)
                    .timestamp(TimeUtils.nowUtc())
                    .build();
                
                log.info("🔍 About to call webSocketHandler.sendNotificationToUser...");
                webSocketHandler.sendNotificationToUser(partnerId.get(), notification);
                log.info("🔍 Called webSocketHandler.sendNotificationToUser - method returned");
            }
        } else {
            log.info("❌ No partner found for user {} - no notification will be sent", post.getUserId());
        }
    }
    
    /**
     * Send new comment notification to post author
     * 
     * @param comment the newly created comment
     * @param post the post being commented on
     */
    public void sendNewCommentNotification(Comment comment, Post post) {
        // Get post author ID
        Long postAuthorId = post.getUserId();
        
        // Don't send notification to self
        if (comment.getUserId().equals(postAuthorId)) {
            return;
        }
        
        Optional<User> commentAuthor = userRepository.findById(comment.getUserId());
        
        if (commentAuthor.isPresent()) {
            String authorName = commentAuthor.get().getNickname() != null ? 
                commentAuthor.get().getNickname() : commentAuthor.get().getUsername();
            
            log.info("💬 New comment notification: {} commented on post", authorName);
            
            // Prepare notification data
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserName", commentAuthor.get().getUsername());
            data.put("fromUserNickname", authorName);
            data.put("postId", post.getPostId());
            data.put("commentId", comment.getCommentId());
            data.put("commentContent", comment.getContent().length() > 50 ? 
                comment.getContent().substring(0, 50) + "..." : comment.getContent());
            
            // Send WebSocket notification
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.Type.NEW_COMMENT)
                .fromUserId(comment.getUserId())
                .toUserId(postAuthorId)
                .message(authorName + " commented on your post")
                .data(data)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(postAuthorId, notification);
        }
    }
    
    /**
     * Send post deleted notification to partner
     * 
     * @param post the deleted post
     */
    public void sendPostDeletedNotification(Post post) {
        Optional<Long> partnerId = userRelationshipService.getPartnerUserId(post.getUserId());
        
        if (partnerId.isPresent()) {
            Optional<User> postAuthor = userRepository.findById(post.getUserId());
            
            if (postAuthor.isPresent()) {
                String authorName = postAuthor.get().getNickname() != null ? 
                    postAuthor.get().getNickname() : postAuthor.get().getUsername();
                
                log.info("🗑️ Post deletion notification: {} deleted a post", authorName);
                
                // Prepare notification data
                Map<String, Object> data = new HashMap<>();
                data.put("fromUserName", postAuthor.get().getUsername());
                data.put("fromUserNickname", authorName);
                data.put("postId", post.getPostId());
                
                // Send WebSocket notification
                NotificationMessage notification = NotificationMessage.builder()
                    .type(NotificationMessage.Type.POST_DELETED)
                    .fromUserId(post.getUserId())
                    .toUserId(partnerId.get())
                    .message(authorName + " deleted a post")
                    .data(data)
                    .timestamp(TimeUtils.nowUtc())
                    .build();
                
                webSocketHandler.sendNotificationToUser(partnerId.get(), notification);
            }
        }
    }
    
    /**
     * Send post updated notification to partner
     * 
     * @param post the updated post
     */
    public void sendPostUpdatedNotification(Post post) {
        Optional<Long> partnerId = userRelationshipService.getPartnerUserId(post.getUserId());
        
        if (partnerId.isPresent()) {
            Optional<User> postAuthor = userRepository.findById(post.getUserId());
            
            if (postAuthor.isPresent()) {
                String authorName = postAuthor.get().getNickname() != null ? 
                    postAuthor.get().getNickname() : postAuthor.get().getUsername();
                
                log.info("✏️ Post update notification: {} updated a post", authorName);
                
                // Prepare notification data
                Map<String, Object> data = new HashMap<>();
                data.put("fromUserName", postAuthor.get().getUsername());
                data.put("fromUserNickname", authorName);
                data.put("postId", post.getPostId());
                data.put("postContent", post.getContent().length() > 50 ? 
                    post.getContent().substring(0, 50) + "..." : post.getContent());
                
                // Send WebSocket notification
                NotificationMessage notification = NotificationMessage.builder()
                    .type(NotificationMessage.Type.POST_UPDATED)
                    .fromUserId(post.getUserId())
                    .toUserId(partnerId.get())
                    .message(authorName + " updated a post")
                    .data(data)
                    .timestamp(TimeUtils.nowUtc())
                    .build();
                
                webSocketHandler.sendNotificationToUser(partnerId.get(), notification);
            }
        }
    }
    
    /**
     * Send comment deleted notification to post author
     * 
     * @param comment the deleted comment
     * @param post the post the comment belonged to
     */
    public void sendCommentDeletedNotification(Comment comment, Post post) {
        // Get post author ID
        Long postAuthorId = post.getUserId();
        
        // Don't send notification to self
        if (comment.getUserId().equals(postAuthorId)) {
            return;
        }
        
        Optional<User> commentAuthor = userRepository.findById(comment.getUserId());
        
        if (commentAuthor.isPresent()) {
            String authorName = commentAuthor.get().getNickname() != null ? 
                commentAuthor.get().getNickname() : commentAuthor.get().getUsername();
            
            log.info("🗑️ Comment deletion notification: {} deleted a comment", authorName);
            
            // Prepare notification data
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserName", commentAuthor.get().getUsername());
            data.put("fromUserNickname", authorName);
            data.put("postId", post.getPostId());
            data.put("commentId", comment.getCommentId());
            
            // Send WebSocket notification
            NotificationMessage notification = NotificationMessage.builder()
                .type(NotificationMessage.Type.COMMENT_DELETED)
                .fromUserId(comment.getUserId())
                .toUserId(postAuthorId)
                .message(authorName + " deleted a comment on your post")
                .data(data)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(postAuthorId, notification);
        }
    }
    
    /**
     * Send relationship ended notification to both users
     * 
     * @param userId1 first user ID
     * @param userId2 second user ID
     */
    public void sendRelationshipEndedNotification(Long userId1, Long userId2) {
        Optional<User> user1 = userRepository.findById(userId1);
        Optional<User> user2 = userRepository.findById(userId2);
        
        if (user1.isPresent() && user2.isPresent()) {
            String user1Name = user1.get().getNickname() != null ? 
                user1.get().getNickname() : user1.get().getUsername();
            String user2Name = user2.get().getNickname() != null ? 
                user2.get().getNickname() : user2.get().getUsername();
            
            log.info("💔 Relationship ended notification: {} and {} ended their relationship", user1Name, user2Name);
            
            // Send notification to first user
            Map<String, Object> data1 = new HashMap<>();
            data1.put("fromUserName", user2.get().getUsername());
            data1.put("fromUserNickname", user2Name);
            
            NotificationMessage notification1 = NotificationMessage.builder()
                .type(NotificationMessage.Type.RELATIONSHIP_ENDED)
                .fromUserId(userId2)
                .toUserId(userId1)
                .message("Your relationship with " + user2Name + " has ended")
                .data(data1)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(userId1, notification1);
            
            // Send notification to second user
            Map<String, Object> data2 = new HashMap<>();
            data2.put("fromUserName", user1.get().getUsername());
            data2.put("fromUserNickname", user1Name);
            
            NotificationMessage notification2 = NotificationMessage.builder()
                .type(NotificationMessage.Type.RELATIONSHIP_ENDED)
                .fromUserId(userId1)
                .toUserId(userId2)
                .message("Your relationship with " + user1Name + " has ended")
                .data(data2)
                .timestamp(TimeUtils.nowUtc())
                .build();
            
            webSocketHandler.sendNotificationToUser(userId2, notification2);
        }
    }
} 