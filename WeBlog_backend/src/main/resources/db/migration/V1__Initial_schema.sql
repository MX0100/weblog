-- ======================================
-- WeBlog Initial Database Schema
-- ======================================
-- Version: 1.0
-- Description: Create initial tables for WeBlog application
-- Date: 2025-01-22

-- ======================================
-- Users Table
-- ======================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'PREFER_NOT_TO_SAY')),
    profileimg TEXT,
    hobby TEXT[], -- PostgreSQL array for hobbies
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- ======================================
-- User Relationships Table
-- ======================================
CREATE TABLE IF NOT EXISTS user_relationships (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    requester_user_id BIGINT NOT NULL,
    relationship_type VARCHAR(20) NOT NULL CHECK (relationship_type IN ('COUPLE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key constraints
    CONSTRAINT fk_user_relationships_user1 FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_relationships_user2 FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_relationships_requester FOREIGN KEY (requester_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate relationships
    CONSTRAINT uk_user_relationships_unique UNIQUE (user1_id, user2_id),
    
    -- Check constraint to ensure user1_id < user2_id (canonical ordering)
    CONSTRAINT chk_user_relationships_order CHECK (user1_id < user2_id),
    
    -- Check constraint to ensure users don't create relationships with themselves
    CONSTRAINT chk_user_relationships_different_users CHECK (user1_id != user2_id)
);

-- Create indexes for user_relationships table
CREATE INDEX IF NOT EXISTS idx_user_relationships_user1 ON user_relationships(user1_id);
CREATE INDEX IF NOT EXISTS idx_user_relationships_user2 ON user_relationships(user2_id);
CREATE INDEX IF NOT EXISTS idx_user_relationships_requester ON user_relationships(requester_user_id);
CREATE INDEX IF NOT EXISTS idx_user_relationships_status ON user_relationships(status);
CREATE INDEX IF NOT EXISTS idx_user_relationships_created_at ON user_relationships(created_at);

-- ======================================
-- Posts Table
-- ======================================
CREATE TABLE IF NOT EXISTS posts (
    post_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT, -- Legacy plain text content
    content_rich JSONB, -- Rich text content in JSON format
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Check constraint to ensure at least one content field is provided
    CONSTRAINT chk_posts_content CHECK (content IS NOT NULL OR content_rich IS NOT NULL)
);

-- Create indexes for posts table
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_updated_at ON posts(updated_at);

-- Create GIN index for rich content search
CREATE INDEX IF NOT EXISTS idx_posts_content_rich_gin ON posts USING GIN (content_rich);

-- ======================================
-- Comments Table
-- ======================================
CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT, -- Legacy plain text content
    content_rich JSONB, -- Rich text content in JSON format
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Check constraint to ensure at least one content field is provided
    CONSTRAINT chk_comments_content CHECK (content IS NOT NULL OR content_rich IS NOT NULL)
);

-- Create indexes for comments table
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at DESC);

-- Create GIN index for rich content search
CREATE INDEX IF NOT EXISTS idx_comments_content_rich_gin ON comments USING GIN (content_rich);

-- ======================================
-- Functions and Triggers
-- ======================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at 
    BEFORE UPDATE ON posts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at 
    BEFORE UPDATE ON comments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ======================================
-- Comments and Documentation
-- ======================================

-- Table comments
COMMENT ON TABLE users IS 'User accounts for the WeBlog application';
COMMENT ON TABLE user_relationships IS 'Relationships between users (couples)';
COMMENT ON TABLE posts IS 'Blog posts created by users';
COMMENT ON TABLE comments IS 'Comments on blog posts';

-- Column comments
COMMENT ON COLUMN users.hobby IS 'Array of user hobbies/interests';
COMMENT ON COLUMN posts.content_rich IS 'Rich text content in JSON format supporting formatting';
COMMENT ON COLUMN comments.content_rich IS 'Rich text content in JSON format supporting formatting';

-- ======================================
-- Initial Data Setup (Optional)
-- ======================================
-- This section can be used for essential data that must exist in production
-- For example: system users, default configurations, etc.
-- Use sparingly and only for truly required data

-- Example system user (commented out - add if needed)
-- INSERT INTO users (username, password, nickname, gender) 
-- VALUES ('system', '$2a$10$...', 'System User', 'PREFER_NOT_TO_SAY')
-- ON CONFLICT (username) DO NOTHING; 