package io.github.mx0100.weblog.entity;

import io.github.mx0100.weblog.utils.TimeUtils;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * UserRelationship entity - manages relationships between users
 * 
 * @author mx0100
 */
@Data
@Entity
@Table(name = "user_relationships", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
@EqualsAndHashCode(callSuper = false)
public class UserRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;
    
    @Column(name = "user2_id", nullable = false)
    private Long user2Id;
    
    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType = RelationshipType.COUPLE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RelationshipStatus status = RelationshipStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    /**
     * Relationship type enum
     */
    public enum RelationshipType {
        COUPLE
    }
    
    /**
     * Relationship status enum
     */
    public enum RelationshipStatus {
        PENDING,   // 待处理
        ACTIVE,    // 活跃
        INACTIVE   // 已结束
    }
    
    /**
     * Pre-persist hook to set UTC timestamp and validate user IDs
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = TimeUtils.nowUtc();
        
        // Set requester if not already set (use the original user1Id before sorting)
        if (requesterUserId == null) {
            requesterUserId = user1Id;
        }
        
        // Ensure user1_id < user2_id for consistency
        if (user1Id != null && user2Id != null && user1Id > user2Id) {
            Long temp = user1Id;
            user1Id = user2Id;
            user2Id = temp;
        }
    }
    
    /**
     * Get the other user ID in the relationship
     * 
     * @param userId the user ID to compare against
     * @return the other user ID, or null if the provided user ID is not part of this relationship
     */
    public Long getOtherUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        if (userId.equals(user1Id)) {
            return user2Id;
        } else if (userId.equals(user2Id)) {
            return user1Id;
        } else {
            return null;
        }
    }
    
    /**
     * Check if a user is part of this relationship
     * 
     * @param userId the user ID to check
     * @return true if the user is part of this relationship
     */
    public boolean containsUser(Long userId) {
        return userId != null && (userId.equals(user1Id) || userId.equals(user2Id));
    }
    
    /**
     * End this relationship
     */
    public void endRelationship() {
        this.status = RelationshipStatus.INACTIVE;
        this.endedAt = TimeUtils.nowUtc();
    }
    
    /**
     * Get the target user ID (the user who received the request)
     * 
     * @return target user ID
     */
    public Long getTargetUserId() {
        if (requesterUserId == null) {
            return null;
        }
        
        if (requesterUserId.equals(user1Id)) {
            return user2Id;
        } else if (requesterUserId.equals(user2Id)) {
            return user1Id;
        } else {
            // This shouldn't happen in normal cases
            return null;
        }
    }
    
    /**
     * Check if a user is the requester of this relationship
     * 
     * @param userId the user ID to check
     * @return true if the user is the requester
     */
    public boolean isRequester(Long userId) {
        return userId != null && userId.equals(requesterUserId);
    }
    
    /**
     * Check if a user is the target (receiver) of this relationship
     * 
     * @param userId the user ID to check
     * @return true if the user is the target
     */
    public boolean isTarget(Long userId) {
        return userId != null && userId.equals(getTargetUserId());
    }
} 