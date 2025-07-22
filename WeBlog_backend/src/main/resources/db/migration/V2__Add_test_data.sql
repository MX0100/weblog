-- ======================================
-- Test Data Migration
-- ======================================
-- Version: 2.0
-- Description: Add sample data for testing
-- Note: This migration demonstrates data preservation during schema updates

-- ======================================
-- Insert Sample Users
-- ======================================
INSERT INTO users (username, password, nickname, gender, profileimg, hobby, created_at, updated_at) VALUES
('alice', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Alice Johnson', 'FEMALE',
 'https://example.com/avatars/alice.jpg', ARRAY['阅读', '旅游', '摄影'], CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('bob', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Bob Smith', 'MALE', 
 'https://example.com/avatars/bob.jpg', ARRAY['编程', '游戏', '音乐'], CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('charlie', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Charlie Chen', 'MALE', 
 'https://example.com/avatars/charlie.jpg', ARRAY['编程', '电影', '美食'], CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('diana', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Diana Wang', 'FEMALE', 
 'https://example.com/avatars/diana.jpg', ARRAY['设计', '艺术', '美食'], CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- ======================================
-- Insert Sample Relationships
-- ======================================
-- Get user IDs for relationships (safer than hardcoded IDs)
DO $$
DECLARE
    charlie_id INTEGER;
    diana_id INTEGER;
BEGIN
    SELECT user_id INTO charlie_id FROM users WHERE username = 'charlie';
    SELECT user_id INTO diana_id FROM users WHERE username = 'diana';
    
    -- Only create relationship if both users exist
    IF charlie_id IS NOT NULL AND diana_id IS NOT NULL THEN
        INSERT INTO user_relationships (user1_id, user2_id, requester_user_id, relationship_type, status, created_at)
        VALUES (
            LEAST(charlie_id, diana_id),
            GREATEST(charlie_id, diana_id), 
            charlie_id, 
            'COUPLE', 
            'ACTIVE', 
            CURRENT_TIMESTAMP - INTERVAL '30 days'
        )
        ON CONFLICT (user1_id, user2_id) DO NOTHING;
    END IF;
END $$;

-- ======================================
-- Insert Sample Posts
-- ======================================
-- Insert posts for users (using username lookup for safety)
DO $$
DECLARE
    alice_id INTEGER;
    bob_id INTEGER;
    charlie_id INTEGER;
    diana_id INTEGER;
BEGIN
    SELECT user_id INTO alice_id FROM users WHERE username = 'alice';
    SELECT user_id INTO bob_id FROM users WHERE username = 'bob';
    SELECT user_id INTO charlie_id FROM users WHERE username = 'charlie';
    SELECT user_id INTO diana_id FROM users WHERE username = 'diana';
    
    -- Alice's posts
    IF alice_id IS NOT NULL THEN
        INSERT INTO posts (user_id, content, content_rich, created_at, updated_at) VALUES
        (alice_id, '今天是我加入这个博客平台的第一天，希望能在这里记录生活的美好时光！', 
         '{"plainText": "今天是我加入这个博客平台的第一天，希望能在这里记录生活的美好时光！", "formattedContent": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "今天是我加入这个博客平台的第一天，希望能在这里记录生活的美好时光！"}]}]}}', 
         CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days');
    END IF;
    
    -- Bob's posts
    IF bob_id IS NOT NULL THEN
        INSERT INTO posts (user_id, content, content_rich, created_at, updated_at) VALUES
        (bob_id, '开始学习Spring Boot，发现这个框架真的很强大！', 
         '{"plainText": "开始学习Spring Boot，发现这个框架真的很强大！", "formattedContent": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "开始学习Spring Boot，发现这个框架真的很强大！"}]}]}}', 
         CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days');
    END IF;
    
    -- Charlie and Diana's posts (couple posts)
    IF charlie_id IS NOT NULL THEN
        INSERT INTO posts (user_id, content, content_rich, created_at, updated_at) VALUES
        (charlie_id, '和Diana一起去了新开的那家日料店，味道真的很棒！', 
         '{"plainText": "和Diana一起去了新开的那家日料店，味道真的很棒！", "formattedContent": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "和Diana一起去了新开的那家日料店，味道真的很棒！"}]}]}}', 
         CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days');
    END IF;
    
    IF diana_id IS NOT NULL THEN
        INSERT INTO posts (user_id, content, content_rich, created_at, updated_at) VALUES
        (diana_id, '今天和Charlie看了一部很感人的电影，两个人都哭了😭', 
         '{"plainText": "今天和Charlie看了一部很感人的电影，两个人都哭了😭", "formattedContent": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "今天和Charlie看了一部很感人的电影，两个人都哭了😭"}]}]}}', 
         CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days');
    END IF;
END $$;

-- ======================================
-- Verification
-- ======================================
-- Log migration results
DO $$
DECLARE
    user_count INTEGER;
    post_count INTEGER;
    relationship_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO user_count FROM users;
    SELECT COUNT(*) INTO post_count FROM posts;
    SELECT COUNT(*) INTO relationship_count FROM user_relationships;
    
    RAISE NOTICE 'Migration V2 completed successfully:';
    RAISE NOTICE 'Users: %', user_count;
    RAISE NOTICE 'Posts: %', post_count;
    RAISE NOTICE 'Relationships: %', relationship_count;
END $$; 