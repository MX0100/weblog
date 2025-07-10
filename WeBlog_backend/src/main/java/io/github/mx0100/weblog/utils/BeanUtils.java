package io.github.mx0100.weblog.utils;

import io.github.mx0100.weblog.dto.response.*;
import io.github.mx0100.weblog.entity.Comment;
import io.github.mx0100.weblog.entity.Post;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.service.UserRelationshipService;

/**
 * Bean mapping utility class
 * Convert between entities and DTOs
 * 
 * @author mx0100
 */
public class BeanUtils {
    
    /**
     * Convert User entity to UserResponse DTO
     * 
     * @param user User entity
     * @return UserResponse DTO
     */
    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setGender(user.getGender());
        response.setProfileimg(user.getProfileimg());
        response.setHobby(user.getHobby());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        return response;
    }
    
    /**
     * Convert User entity to UserResponse DTO with relationship status
     * 
     * @param user User entity
     * @param relationshipStatus relationship status information
     * @return UserResponse DTO
     */
    public static UserResponse toUserResponse(User user, UserRelationshipService.RelationshipStatusInfo relationshipStatus) {
        if (user == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setGender(user.getGender());
        response.setProfileimg(user.getProfileimg());
        response.setHobby(user.getHobby());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // Set relationship status
        if (relationshipStatus != null) {
            response.setRelationshipStatus(relationshipStatus.getStatus());
            response.setPartnerId(relationshipStatus.getPartnerId());
        } else {
            response.setRelationshipStatus("SINGLE");
            response.setPartnerId(null);
        }
        
        return response;
    }
    
    /**
     * Convert Post entity to PostResponse DTO
     * 
     * @param post Post entity
     * @param author User entity (author)
     * @return PostResponse DTO
     */
    public static PostResponse toPostResponse(Post post, User author) {
        return toPostResponse(post, author, null, null);
    }
    
    /**
     * Convert Post entity to PostResponse DTO with comment information
     * Supports rich text content
     * 
     * @param post Post entity
     * @param author User entity (author)
     * @param commentIds List of comment IDs for this post
     * @param commentsCount Total number of comments for this post
     * @return PostResponse DTO
     */
    public static PostResponse toPostResponse(Post post, User author, java.util.List<Long> commentIds, Integer commentsCount) {
        if (post == null) {
            return null;
        }
        
        PostResponse response = new PostResponse();
        response.setPostId(post.getPostId());
        response.setUserId(post.getUserId());
        
        // Map rich text content
        response.setRichContent(post.getRichContent());
        
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        
        // Set author info
        if (author != null) {
            PostResponse.AuthorInfo authorInfo = new PostResponse.AuthorInfo();
            authorInfo.setUserId(author.getUserId());
            authorInfo.setUsername(author.getUsername());
            authorInfo.setNickname(author.getNickname());
            response.setAuthor(authorInfo);
        }
        
        // Set comment information
        response.setComments(commentIds != null ? commentIds : new java.util.ArrayList<>());
        response.setCommentsCount(commentsCount != null ? commentsCount : 0);
        
        return response;
    }
    
    /**
     * Convert Comment entity to CommentResponse DTO
     * Supports rich text content
     * 
     * @param comment Comment entity
     * @param author User entity (author)
     * @return CommentResponse DTO
     */
    public static CommentResponse toCommentResponse(Comment comment, User author) {
        if (comment == null) {
            return null;
        }
        
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setPostId(comment.getPostId());
        response.setUserId(comment.getUserId());
        
        // Map rich text content
        response.setRichContent(comment.getRichContent());
        
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        
        // Set author info
        if (author != null) {
            CommentResponse.AuthorInfo authorInfo = new CommentResponse.AuthorInfo();
            authorInfo.setUserId(author.getUserId());
            authorInfo.setUsername(author.getUsername());
            authorInfo.setNickname(author.getNickname());
            response.setAuthor(authorInfo);
        }
        
        return response;
    }
    
    /**
     * Create LoginResponse DTO
     * 
     * @param token JWT token
     * @param user User entity
     * @return LoginResponse DTO
     */
    public static LoginResponse toLoginResponse(String token, User user) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toUserResponse(user));
        return response;
    }
    
    /**
     * Create LoginResponse DTO with relationship status
     * 
     * @param token JWT token
     * @param user User entity
     * @param relationshipStatus relationship status information
     * @return LoginResponse DTO
     */
    public static LoginResponse toLoginResponse(String token, User user, UserRelationshipService.RelationshipStatusInfo relationshipStatus) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toUserResponse(user, relationshipStatus));
        return response;
    }
} 