package io.github.mx0100.weblog.controller;

import io.github.mx0100.weblog.common.ApiResponse;
import io.github.mx0100.weblog.dto.request.PairRequest;
import io.github.mx0100.weblog.dto.request.PartnerUsernameRequest;
import io.github.mx0100.weblog.dto.response.RelationshipHistoryResponse;
import io.github.mx0100.weblog.entity.User;
import io.github.mx0100.weblog.entity.UserRelationship;
import io.github.mx0100.weblog.repository.UserRepository;
import io.github.mx0100.weblog.security.UserPrincipal;
import io.github.mx0100.weblog.service.UserRelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Relationship controller
 * Handle user relationship management operations
 * 
 * @author mx0100
 */
@Slf4j
@RestController
@RequestMapping("/api/relationships")
@RequiredArgsConstructor
public class RelationshipController {
    
    private final UserRelationshipService userRelationshipService;
    private final UserRepository userRepository;
    
    /**
     * Send pair request to another user
     * 
     * @param userId current user ID (from path)
     * @param request pair request containing target user ID
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @PostMapping("/pair-request")
    public ApiResponse<Void> sendPairRequest(@Valid @RequestBody PairRequest request,
                                             @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userRelationshipService.sendPairRequest(userPrincipal.getUserId(), request.getPartnerUsername());
        return ApiResponse.success();
    }
    
    /**
     * Accept pair request from another user
     * 
     * @param request accept pair request containing requesting user's username
     * @param userPrincipal current authenticated user
     * @return relationship response
     */
    @PostMapping("/accept-request")
    public ApiResponse<UserRelationship> acceptPairRequest(@Valid @RequestBody PartnerUsernameRequest request,
                                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long currentUserId = userPrincipal.getUserId();
        log.info("Accept pair request by user: {} from user: {}", currentUserId, request.getPartnerUsername());
        
        UserRelationship relationship = userRelationshipService.acceptPairRequest(currentUserId, request.getPartnerUsername());
        return ApiResponse.success(relationship);
    }
    
    /**
     * End current relationship (unpair)
     * 
     * @param userId user ID (from path)
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @DeleteMapping("/{userId}/unpair")
    public ApiResponse<Void> unpairUser(@PathVariable Long userId,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // Verify the path userId matches the authenticated user
        if (!userId.equals(userPrincipal.getUserId())) {
            throw new RuntimeException("Permission denied: can only unpair yourself");
        }
        
        log.info("Unpair request by user: {}", userId);
        
        userRelationshipService.unpairUser(userId);
        return ApiResponse.success();
    }
    
    /**
     * Get relationship history for the user
     * 
     * @param userId user ID (from path)
     * @param userPrincipal current authenticated user
     * @return list of relationship history
     */
    @GetMapping("/{userId}/relationship-history")
    public ApiResponse<List<RelationshipHistoryResponse>> getRelationshipHistory(@PathVariable Long userId,
                                                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        // Verify the path userId matches the authenticated user
        if (!userId.equals(userPrincipal.getUserId())) {
            throw new RuntimeException("Permission denied: can only view your own relationship history");
        }
        
        log.info("Get relationship history for user: {}", userId);
        
        List<UserRelationship> relationships = userRelationshipService.getRelationshipHistory(userId);
        
        // Convert to response DTOs
        List<RelationshipHistoryResponse> history = relationships.stream()
                .map(relationship -> {
                    RelationshipHistoryResponse response = new RelationshipHistoryResponse();
                    response.setId(relationship.getId());
                    response.setRelationshipType(relationship.getRelationshipType().name());
                    response.setStatus(relationship.getStatus().name());
                    response.setCreatedAt(relationship.getCreatedAt());
                    response.setEndedAt(relationship.getEndedAt());
                    
                    // Get partner information
                    Long partnerId = relationship.getOtherUserId(userId);
                    if (partnerId != null) {
                        Optional<User> partnerOpt = userRepository.findById(partnerId);
                        if (partnerOpt.isPresent()) {
                            User partner = partnerOpt.get();
                            response.setPartnerId(partnerId);
                            response.setPartnerUsername(partner.getUsername());
                            response.setPartnerNickname(partner.getNickname());
                        } else {
                            log.warn("Failed to load partner info for relationship {}", relationship.getId());
                            response.setPartnerId(partnerId);
                            response.setPartnerUsername("Unknown");
                            response.setPartnerNickname("Unknown");
                        }
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
        
        return ApiResponse.success(history);
    }
    
    /**
     * Get pending pair requests (requests received by the user)
     * 
     * @param userPrincipal current authenticated user
     * @return list of pending pair requests
     */
    @GetMapping("/pending")
    public ApiResponse<List<RelationshipHistoryResponse>> getPendingPairRequests(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        log.info("Get pending pair requests for user: {}", userId);
        
        List<UserRelationship> pendingRequests = userRelationshipService.getPendingPairRequests(userId);
        
        // Convert to response DTOs
        List<RelationshipHistoryResponse> responses = pendingRequests.stream()
                .map(relationship -> {
                    RelationshipHistoryResponse response = new RelationshipHistoryResponse();
                    response.setId(relationship.getId());
                    response.setRelationshipType(relationship.getRelationshipType().name());
                    response.setStatus(relationship.getStatus().name());
                    response.setCreatedAt(relationship.getCreatedAt());
                    response.setEndedAt(relationship.getEndedAt());
                    
                    // Get requester information (the user who sent the request)
                    Long requesterId = relationship.getRequesterUserId();
                    if (requesterId != null) {
                        Optional<User> requesterOpt = userRepository.findById(requesterId);
                        if (requesterOpt.isPresent()) {
                            User requester = requesterOpt.get();
                            response.setPartnerId(requesterId);
                            response.setPartnerUsername(requester.getUsername());
                            response.setPartnerNickname(requester.getNickname());
                        }
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * Get sent pair requests (requests sent by the user)
     * 
     * @param userPrincipal current authenticated user
     * @return list of sent pair requests
     */
    @GetMapping("/sent")
    public ApiResponse<List<RelationshipHistoryResponse>> getSentPairRequests(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getUserId();
        log.info("Get sent pair requests for user: {}", userId);
        
        List<UserRelationship> sentRequests = userRelationshipService.getSentPairRequests(userId);
        
        // Convert to response DTOs
        List<RelationshipHistoryResponse> responses = sentRequests.stream()
                .map(relationship -> {
                    RelationshipHistoryResponse response = new RelationshipHistoryResponse();
                    response.setId(relationship.getId());
                    response.setRelationshipType(relationship.getRelationshipType().name());
                    response.setStatus(relationship.getStatus().name());
                    response.setCreatedAt(relationship.getCreatedAt());
                    response.setEndedAt(relationship.getEndedAt());
                    
                    // Get target information (the user who received the request)
                    Long targetId = relationship.getTargetUserId();
                    if (targetId != null) {
                        Optional<User> targetOpt = userRepository.findById(targetId);
                        if (targetOpt.isPresent()) {
                            User target = targetOpt.get();
                            response.setPartnerId(targetId);
                            response.setPartnerUsername(target.getUsername());
                            response.setPartnerNickname(target.getNickname());
                        }
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }
    
    /**
     * Reject pair request from another user
     *
     * @param request reject pair request containing requesting user's username
     * @param userPrincipal current authenticated user
     * @return success response
     */
    @PostMapping("/reject-request")
    public ApiResponse<Void> rejectPairRequest(@Valid @RequestBody PartnerUsernameRequest request,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getUserId();
        log.info("Reject pair request by user: {} from user: {}", currentUserId, request.getPartnerUsername());

        userRelationshipService.rejectPairRequest(currentUserId, request.getPartnerUsername());
        return ApiResponse.success();
    }
} 