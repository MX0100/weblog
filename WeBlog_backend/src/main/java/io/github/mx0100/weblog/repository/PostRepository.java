package io.github.mx0100.weblog.repository;

import io.github.mx0100.weblog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Post repository interface
 * 
 * @author mx0100
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * Find posts by user ID with pagination
     * 
     * @param userId user ID
     * @param pageable pageable
     * @return page of posts
     */
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find posts by multiple user IDs with pagination (for couples)
     * 
     * @param userIds list of user IDs
     * @param pageable pageable
     * @return page of posts
     */
    Page<Post> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
    
    /**
     * Find all posts with pagination ordered by creation time desc
     * 
     * @param pageable pageable
     * @return page of posts
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Count posts by user ID
     * 
     * @param userId user ID
     * @return post count
     */
    long countByUserId(Long userId);
    
    /**
     * Find posts by multiple user IDs without pagination
     * 
     * @param userIds list of user IDs
     * @return list of posts
     */
    List<Post> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds);
} 