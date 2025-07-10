package io.github.mx0100.weblog.service;

import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.entity.UserRelationship;
import io.github.mx0100.weblog.repository.UserRelationshipRepository;
import io.github.mx0100.weblog.repository.UserRepository;
import io.github.mx0100.weblog.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserRelationship service for managing user relationships
 * 
 * @author mx0100
 */
@Slf4j
@Service
public class UserRelationshipService {
    
    private final UserRelationshipRepository userRelationshipRepository;
    private final UserRepository userRepository;
    
    @Autowired
    @Lazy
    private NotificationService notificationService;
    
    // Manual constructor to inject only non-lazy dependencies
    public UserRelationshipService(UserRelationshipRepository userRelationshipRepository, 
                                   UserRepository userRepository) {
        this.userRelationshipRepository = userRelationshipRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get user's partner ID if they have an active relationship
     * 
     * @param userId user ID
     * @return partner user ID or null if single
     */
    public Optional<Long> getPartnerUserId(Long userId) {
        return userRelationshipRepository.findActiveRelationshipByUserId(userId)
                .map(relationship -> relationship.getOtherUserId(userId));
    }
    
    /**
     * Get user's relationship status and partner info
     * 
     * @param userId user ID
     * @return relationship status info
     */
    public RelationshipStatusInfo getRelationshipStatus(Long userId) {
        Optional<UserRelationship> relationship = userRelationshipRepository.findActiveRelationshipByUserId(userId);
        
        if (relationship.isPresent()) {
            Long partnerId = relationship.get().getOtherUserId(userId);
            Optional<User> partner = userRepository.findById(partnerId);
            
            return RelationshipStatusInfo.builder()
                    .status("COUPLED")
                    .partnerId(partnerId)
                    .partnerUsername(partner.map(User::getUsername).orElse(null))
                    .partnerNickname(partner.map(User::getNickname).orElse(null))
                    .relationshipId(relationship.get().getId())
                    .build();
        } else {
            return RelationshipStatusInfo.builder()
                    .status("SINGLE")
                    .partnerId(null)
                    .partnerUsername(null)
                    .partnerNickname(null)
                    .relationshipId(null)
                    .build();
        }
    }
    
    /**
     * Overloaded method to send pair request using username
     * 
     * @param fromUserId       requesting user ID
     * @param partnerUsername  target user's username
     * @return created relationship
     */
    @Transactional
    public UserRelationship sendPairRequest(Long fromUserId, String partnerUsername) {
        // Find the target user by username
        User toUser = userRepository.findByUsername(partnerUsername)
                .orElseThrow(() -> new RuntimeException("User with username '" + partnerUsername + "' not found."));
        
        // Delegate to the original ID-based method
        return sendPairRequest(fromUserId, toUser.getUserId());
    }

    /**
     * Send pair request (create PENDING relationship between two users)
     * This method is now private and handles the core logic.
     * 
     * @param fromUserId requesting user ID
     * @param toUserId target user ID
     * @return created relationship
     * @throws IllegalArgumentException if users cannot be paired
     */
    @Transactional
    private UserRelationship sendPairRequest(Long fromUserId, Long toUserId) {
        // Validate users exist
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("Requesting user does not exist."));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Target user does not exist."));

        // Cannot pair with yourself
        if (fromUser.getUserId().equals(toUser.getUserId())) {
            throw new RuntimeException("You cannot send a pair request to yourself.");
        }
        
        // Check if either user already has an active relationship
        if (userRelationshipRepository.hasActiveRelationship(fromUserId)) {
            throw new RuntimeException("You are already in a relationship.");
        }
        if (userRelationshipRepository.hasActiveRelationship(toUserId)) {
            throw new RuntimeException("Target user '" + toUser.getUsername() + "' is already in a relationship.");
        }
        
        // Check if there's already a pending request between these users
        if (userRelationshipRepository.existsPendingRelationshipBetweenUsers(fromUserId, toUserId)) {
            throw new RuntimeException("A pair request between you and '" + toUser.getUsername() + "' already exists.");
        }
        
        // Check if there's any existing relationship between these users
        Optional<UserRelationship> existingRelationship = userRelationshipRepository.findAnyRelationshipBetweenUsers(fromUserId, toUserId);
        
        UserRelationship relationship;
        if (existingRelationship.isPresent()) {
            // Update existing relationship
            relationship = existingRelationship.get();
            
            // Check the current status
            if (relationship.getStatus() == UserRelationship.RelationshipStatus.ACTIVE) {
                throw new RuntimeException("You are already paired with this user.");
            }
            if (relationship.getStatus() == UserRelationship.RelationshipStatus.PENDING) {
                throw new RuntimeException("A pair request already exists with this user.");
            }
            
            // Reactivate INACTIVE relationship
            relationship.setRequesterUserId(fromUserId);
            relationship.setStatus(UserRelationship.RelationshipStatus.PENDING);
            relationship.setCreatedAt(TimeUtils.nowUtc());
            relationship.setEndedAt(null);
            
            log.info("Reactivated existing relationship request from user {} to user {}", fromUserId, toUserId);
        } else {
            // Create new PENDING relationship (user IDs will be automatically ordered by entity)
            relationship = new UserRelationship();
            relationship.setUser1Id(fromUserId);
            relationship.setUser2Id(toUserId);
            relationship.setRequesterUserId(fromUserId);  // Explicitly set the requester
            relationship.setRelationshipType(UserRelationship.RelationshipType.COUPLE);
            relationship.setStatus(UserRelationship.RelationshipStatus.PENDING);
            
            log.info("Created new PENDING relationship request from user {} to user {}", fromUserId, toUserId);
        }
        
        UserRelationship savedRelationship = userRelationshipRepository.save(relationship);
        
        // Send notification to target user
        notificationService.sendPairRequestNotification(fromUserId, toUserId);
        
        return savedRelationship;
    }
    
    /**
     * Overloaded method to accept a pair request using username.
     *
     * @param acceptingUserId The ID of the user accepting the request.
     * @param requesterUsername The username of the user who sent the request.
     * @return The updated, active relationship.
     */
    @Transactional
    public UserRelationship acceptPairRequest(Long acceptingUserId, String requesterUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester with username '" + requesterUsername + "' not found."));
        return acceptPairRequest(acceptingUserId, requester.getUserId());
    }

    /**
     * Accept pair request (change PENDING relationship to ACTIVE)
     * This is now the private core logic method.
     * 
     * @param userId accepting user ID
     * @param requestUserId requesting user ID
     * @return accepted relationship
     */
    @Transactional
    private UserRelationship acceptPairRequest(Long userId, Long requestUserId) {
        // Validate users exist
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Accepting user does not exist");
        }
        if (!userRepository.existsById(requestUserId)) {
            throw new IllegalArgumentException("Requesting user does not exist");
        }
        
        // Find the pending relationship
        Optional<UserRelationship> pendingRelationship = userRelationshipRepository
            .findPendingRelationshipBetweenUsers(requestUserId, userId);
        
        if (pendingRelationship.isEmpty()) {
            throw new IllegalArgumentException("No pending relationship found between these users");
        }
        
        UserRelationship relationship = pendingRelationship.get();
        
        // Change status to ACTIVE
        relationship.setStatus(UserRelationship.RelationshipStatus.ACTIVE);
        UserRelationship savedRelationship = userRelationshipRepository.save(relationship);
        
        log.info("Accepted pair request: user {} accepted request from user {}", userId, requestUserId);
        
        // Send notification to requesting user
        notificationService.sendPairRequestAcceptedNotification(requestUserId, userId);
        
        return savedRelationship;
    }
    
    /**
     * Overloaded method to reject a pair request using username.
     *
     * @param rejectingUserId The ID of the user rejecting the request.
     * @param requesterUsername The username of the user who sent the request.
     */
    @Transactional
    public void rejectPairRequest(Long rejectingUserId, String requesterUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("Requester with username '" + requesterUsername + "' not found."));
        rejectPairRequest(rejectingUserId, requester.getUserId());
    }

    /**
     * Reject pair request (change PENDING relationship to INACTIVE)
     * This is now the private core logic method.
     * 
     * @param userId rejecting user ID
     * @param requestUserId requesting user ID
     */
    @Transactional
    private void rejectPairRequest(Long userId, Long requestUserId) {
        // Validate users exist
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Rejecting user does not exist");
        }
        if (!userRepository.existsById(requestUserId)) {
            throw new IllegalArgumentException("Requesting user does not exist");
        }
        
        // Find the pending relationship
        Optional<UserRelationship> pendingRelationship = userRelationshipRepository
            .findPendingRelationshipBetweenUsers(requestUserId, userId);
        
        if (pendingRelationship.isEmpty()) {
            throw new IllegalArgumentException("No pending relationship found between these users");
        }
        
        UserRelationship relationship = pendingRelationship.get();
        
        // Change status to INACTIVE
        relationship.setStatus(UserRelationship.RelationshipStatus.INACTIVE);
        relationship.endRelationship();
        userRelationshipRepository.save(relationship);
        
        log.info("Rejected pair request: user {} rejected request from user {}", userId, requestUserId);
        
        // Send notification to requesting user
        notificationService.sendPairRequestRejectedNotification(requestUserId, userId);
    }
    
    /**
     * End relationship for a user
     * 
     * @param userId user ID
     * @throws IllegalArgumentException if user has no active relationship
     */
    @Transactional
    public void unpairUser(Long userId) {
        Optional<UserRelationship> relationship = userRelationshipRepository.findActiveRelationshipByUserId(userId);
        
        if (relationship.isEmpty()) {
            throw new IllegalArgumentException("User has no active relationship to end");
        }
        
        UserRelationship rel = relationship.get();
        Long partnerId = rel.getOtherUserId(userId);
        
        rel.endRelationship();
        userRelationshipRepository.save(rel);
        
        log.info("Ended relationship between users {} and {}", userId, partnerId);
        
        // Send notification to both users about relationship ended
        notificationService.sendRelationshipEndedNotification(userId, partnerId);
    }
    
    /**
     * Get user's relationship history
     * 
     * @param userId user ID
     * @return list of relationship history
     */
    public List<UserRelationship> getRelationshipHistory(Long userId) {
        return userRelationshipRepository.findAllRelationshipsByUserId(userId);
    }
    
    /**
     * Get pending pair requests for a user (requests received by this user)
     * 
     * @param userId user ID
     * @return list of pending relationships where this user is the target
     */
    public List<UserRelationship> getPendingPairRequests(Long userId) {
        return userRelationshipRepository.findPendingRelationshipsByTargetUserId(userId);
    }
    
    /**
     * Get sent pair requests for a user (requests sent by this user)
     * 
     * @param userId user ID
     * @return list of pending relationships where this user is the requester
     */
    public List<UserRelationship> getSentPairRequests(Long userId) {
        return userRelationshipRepository.findPendingRelationshipsByRequesterUserId(userId);
    }
    
    /**
     * Check if two users are in an active relationship
     * 
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return true if they are coupled
     */
    public boolean areUsersCoupled(Long userId1, Long userId2) {
        return userRelationshipRepository.existsActiveRelationshipBetweenUsers(userId1, userId2);
    }
    
    /**
     * Check if user can access another user's content
     * 
     * @param viewerUserId user trying to view content
     * @param contentOwnerUserId owner of the content
     * @return true if access is allowed
     */
    public boolean canUserAccessContent(Long viewerUserId, Long contentOwnerUserId) {
        // User can always access their own content
        if (viewerUserId.equals(contentOwnerUserId)) {
            return true;
        }
        
        // User can access their partner's content
        return areUsersCoupled(viewerUserId, contentOwnerUserId);
    }
    
    /**
     * Relationship status information DTO
     */
    public static class RelationshipStatusInfo {
        private String status;
        private Long partnerId;
        private String partnerUsername;
        private String partnerNickname;
        private Long relationshipId;
        
        public static RelationshipStatusInfoBuilder builder() {
            return new RelationshipStatusInfoBuilder();
        }
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getPartnerId() { return partnerId; }
        public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
        
        public String getPartnerUsername() { return partnerUsername; }
        public void setPartnerUsername(String partnerUsername) { this.partnerUsername = partnerUsername; }
        
        public String getPartnerNickname() { return partnerNickname; }
        public void setPartnerNickname(String partnerNickname) { this.partnerNickname = partnerNickname; }
        
        public Long getRelationshipId() { return relationshipId; }
        public void setRelationshipId(Long relationshipId) { this.relationshipId = relationshipId; }
        
        public static class RelationshipStatusInfoBuilder {
            private String status;
            private Long partnerId;
            private String partnerUsername;
            private String partnerNickname;
            private Long relationshipId;
            
            public RelationshipStatusInfoBuilder status(String status) {
                this.status = status;
                return this;
            }
            
            public RelationshipStatusInfoBuilder partnerId(Long partnerId) {
                this.partnerId = partnerId;
                return this;
            }
            
            public RelationshipStatusInfoBuilder partnerUsername(String partnerUsername) {
                this.partnerUsername = partnerUsername;
                return this;
            }
            
            public RelationshipStatusInfoBuilder partnerNickname(String partnerNickname) {
                this.partnerNickname = partnerNickname;
                return this;
            }
            
            public RelationshipStatusInfoBuilder relationshipId(Long relationshipId) {
                this.relationshipId = relationshipId;
                return this;
            }
            
            public RelationshipStatusInfo build() {
                RelationshipStatusInfo info = new RelationshipStatusInfo();
                info.status = this.status;
                info.partnerId = this.partnerId;
                info.partnerUsername = this.partnerUsername;
                info.partnerNickname = this.partnerNickname;
                info.relationshipId = this.relationshipId;
                return info;
            }
        }
    }
} 