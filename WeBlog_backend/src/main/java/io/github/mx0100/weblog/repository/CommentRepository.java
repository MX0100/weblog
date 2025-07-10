package io.github.mx0100.weblog.repository;

import io.github.mx0100.weblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Comment repository interface
 * 
 * @author mx0100
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Find comments by post ID ordered by creation time asc
     * 
     * @param postId post ID
     * @return list of comments
     */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    /**
     * Find comments by post ID with pagination
     * 
     * @param postId post ID
     * @param pageable pageable
     * @return page of comments
     */
    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
    
    /**
     * Find comments by post ID list
     * 
     * @param postIds post ID list
     * @return list of comments
     */
    List<Comment> findByPostIdIn(List<Long> postIds);
    
    /**
     * Find comments by comment ID list
     * 
     * @param commentIds comment ID list
     * @return list of comments
     */
    List<Comment> findByCommentIdIn(List<Long> commentIds);
    
    /**
     * Find comments by user ID
     * 
     * @param userId user ID
     * @return list of comments
     */
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Count comments by post ID
     * 
     * @param postId post ID
     * @return comment count
     */
    long countByPostId(Long postId);
    
    /**
     * Count comments by user ID
     * 
     * @param userId user ID
     * @return comment count
     */
    long countByUserId(Long userId);
    
    /**
     * Delete comments by post ID
     * 
     * @param postId post ID
     */
    void deleteByPostId(Long postId);
    
    /**
     * Delete comments by user ID
     * 
     * @param userId user ID
     */
    void deleteByUserId(Long userId);
} 