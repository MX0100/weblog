package io.github.mx0100.weblog.repository;

import io.github.mx0100.weblog.entity.UserRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRelationship repository interface
 * 
 * @author mx0100
 */
@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Long> {
    
    /**
     * Find active relationship for a user
     * 
     * @param userId user ID
     * @return active relationship optional
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE (ur.user1Id = :userId OR ur.user2Id = :userId) AND ur.status = 'ACTIVE'")
    Optional<UserRelationship> findActiveRelationshipByUserId(@Param("userId") Long userId);
    
    /**
     * Find all relationships (active and inactive) for a user
     * 
     * @param userId user ID
     * @return list of relationships
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE (ur.user1Id = :userId OR ur.user2Id = :userId) ORDER BY ur.createdAt DESC")
    List<UserRelationship> findAllRelationshipsByUserId(@Param("userId") Long userId);
    
    /**
     * Find active relationship between two users
     * 
     * @param user1Id first user ID (should be smaller)
     * @param user2Id second user ID (should be larger)
     * @return relationship optional
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE ur.user1Id = :user1Id AND ur.user2Id = :user2Id AND ur.status = 'ACTIVE'")
    Optional<UserRelationship> findActiveRelationshipBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    /**
     * Check if two users have an active relationship
     * 
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return true if they have an active relationship
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRelationship ur WHERE " +
           "((ur.user1Id = :userId1 AND ur.user2Id = :userId2) OR " +
           "(ur.user1Id = :userId2 AND ur.user2Id = :userId1)) AND ur.status = 'ACTIVE'")
    boolean existsActiveRelationshipBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * Check if user has any active relationship
     * 
     * @param userId user ID
     * @return true if user has an active relationship
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRelationship ur WHERE (ur.user1Id = :userId OR ur.user2Id = :userId) AND ur.status = 'ACTIVE'")
    boolean hasActiveRelationship(@Param("userId") Long userId);
    
    /**
     * Check if two users have a pending relationship
     * 
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return true if they have a pending relationship
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRelationship ur WHERE " +
           "((ur.user1Id = :userId1 AND ur.user2Id = :userId2) OR " +
           "(ur.user1Id = :userId2 AND ur.user2Id = :userId1)) AND ur.status = 'PENDING'")
    boolean existsPendingRelationshipBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * Find pending relationship between two users
     * 
     * @param requestUserId requesting user ID
     * @param targetUserId target user ID
     * @return pending relationship optional
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "((ur.user1Id = :requestUserId AND ur.user2Id = :targetUserId) OR " +
           "(ur.user1Id = :targetUserId AND ur.user2Id = :requestUserId)) AND ur.status = 'PENDING'")
    Optional<UserRelationship> findPendingRelationshipBetweenUsers(@Param("requestUserId") Long requestUserId, @Param("targetUserId") Long targetUserId);
    
    /**
     * Find pending relationships where user is the target (received requests)
     * 
     * @param targetUserId target user ID
     * @return list of pending relationships
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.requesterUserId != :targetUserId AND " +
           "(ur.user1Id = :targetUserId OR ur.user2Id = :targetUserId) AND " +
           "ur.status = 'PENDING' ORDER BY ur.createdAt DESC")
    List<UserRelationship> findPendingRelationshipsByTargetUserId(@Param("targetUserId") Long targetUserId);
    
    /**
     * Find pending relationships where user is the requester (sent requests)
     * 
     * @param requestUserId requesting user ID
     * @return list of pending relationships
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.requesterUserId = :requestUserId AND ur.status = 'PENDING' " +
           "ORDER BY ur.createdAt DESC")
    List<UserRelationship> findPendingRelationshipsByRequesterUserId(@Param("requestUserId") Long requestUserId);
    
    /**
     * Find any relationship (any status) between two users
     * 
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return relationship optional
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "((ur.user1Id = :userId1 AND ur.user2Id = :userId2) OR " +
           "(ur.user1Id = :userId2 AND ur.user2Id = :userId1))")
    Optional<UserRelationship> findAnyRelationshipBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
} 