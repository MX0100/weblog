package io.github.mx0100.weblog.entity;

import io.github.mx0100.weblog.entity.enums.Gender;
import io.github.mx0100.weblog.utils.TimeUtils;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User entity
 * 
 * @author mx0100
 */
@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = false)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username", unique = true, nullable = false, length = 20)
    private String username;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;
    
    @Column(name = "profileimg")
    private String profileimg;
    
    @Column(name = "hobby", columnDefinition = "text[]")
    private List<String> hobby;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Pre-persist hook to set UTC timestamps
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = TimeUtils.nowUtc();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    /**
     * Pre-update hook to set UTC timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeUtils.nowUtc();
    }
} 