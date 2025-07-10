-- Insert test data for Couple Blog System
-- Password for all users is: 123456 (BCrypt encoded: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.)

-- Insert test users
INSERT INTO users (user_id, username, password, nickname, gender, profileimg, hobby, created_at, updated_at) VALUES
-- Single users (will have relationshipStatus = 'SINGLE', partnerId = null)
(1, 'alice', '12345', 'Alice Johnson', 'FEMALE',
 'https://example.com/avatars/alice.jpg', ARRAY['阅读', '旅游', '摄影'], NOW(), NOW()),

(2, 'bob', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Bob Smith', 'MALE', 
 'https://example.com/avatars/bob.jpg', ARRAY['编程', '游戏', '音乐'], NOW(), NOW()),

-- Users in relationships (will have relationshipStatus = 'COUPLED')
(3, 'charlie', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Charlie Chen', 'MALE', 
 'https://example.com/avatars/charlie.jpg', ARRAY['编程', '电影', '美食'], NOW(), NOW()),

(4, 'diana', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Diana Wang', 'FEMALE', 
 'https://example.com/avatars/diana.jpg', ARRAY['设计', '艺术', '美食'], NOW(), NOW()),

-- Another couple
(5, 'edward', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Edward Lee', 'MALE', 
 'https://example.com/avatars/edward.jpg', ARRAY['运动', '健身', '旅游'], NOW(), NOW()),

(6, 'fiona', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Fiona Zhang', 'FEMALE', 
 'https://example.com/avatars/fiona.jpg', ARRAY['瑜伽', '读书', '旅游'], NOW(), NOW());

-- 重置用户ID序列，确保新用户注册时使用正确的ID
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));

-- Insert user relationships (couples)
INSERT INTO user_relationships (user1_id, user2_id, requester_user_id, relationship_type, status, created_at) VALUES
-- Charlie (3) and Diana (4) are a couple (Charlie was the requester)
(3, 4, 3, 'COUPLE', 'ACTIVE', NOW() - INTERVAL '30 days'),

-- Edward (5) and Fiona (6) are a couple (Edward was the requester)
(5, 6, 5, 'COUPLE', 'ACTIVE', NOW() - INTERVAL '60 days');

-- Insert test posts
INSERT INTO posts (post_id, user_id, content, created_at, updated_at) VALUES
-- Single user Alice's posts (only visible to Alice)
(1, 1, '今天是我加入这个博客平台的第一天，希望能在这里记录生活的美好时光！', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(2, 1, '刚读完一本很棒的书《小王子》，深深被感动了。每个大人心中都有一个小王子。', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

-- Single user Bob's posts (only visible to Bob)
(3, 2, '开始学习Spring Boot，发现这个框架真的很强大！自动配置省去了很多繁琐的工作。', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(4, 2, '今天在公园跑步，天气很好。运动真的能让人心情愉悦！', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- Couple Charlie & Diana's posts (visible to both Charlie and Diana)
(5, 3, '和Diana一起去了新开的那家日料店，味道真的很棒！两个人一起探索美食真的很有意思。', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
(6, 4, '今天和Charlie看了一部很感人的电影，两个人都哭了😭。有人一起分享感动的时刻真好。', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(7, 3, '正在学习新的编程技术，Diana总是很支持我的学习，给我很大的动力！', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(8, 4, '最近在设计一个新的UI项目，Charlie给了我很多技术上的建议，我们真的是很好的搭档。', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

-- Couple Edward & Fiona's posts (visible to both Edward and Fiona)
(9, 5, '和Fiona一起去健身房锻炼，她的瑜伽姿势真的很标准，我还需要多多学习。', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(10, 6, '今天Edward陪我去书店，虽然他不太爱看书，但还是很耐心地陪着我。感动💕', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 重置帖子ID序列
SELECT setval('posts_post_id_seq', (SELECT MAX(post_id) FROM posts));

-- Insert test comments
INSERT INTO comments (comment_id, post_id, user_id, content, created_at, updated_at) VALUES
-- Comments on couple posts (couples can comment on each other's posts)
(1, 5, 4, '那家店真的很棒！我们下次再去试试其他菜品～', NOW() - INTERVAL '6 days' + INTERVAL '2 hours', NOW() - INTERVAL '6 days' + INTERVAL '2 hours'),
(2, 6, 3, '电影确实很感人，能和你一起看这样的电影我也很开心😊', NOW() - INTERVAL '4 days' + INTERVAL '1 hour', NOW() - INTERVAL '4 days' + INTERVAL '1 hour'),
(3, 7, 4, '你学习很认真，我也要向你学习这种专注的精神！', NOW() - INTERVAL '2 days' + INTERVAL '3 hours', NOW() - INTERVAL '2 days' + INTERVAL '3 hours'),
(4, 8, 3, '你的设计天赋真的很棒，我只是提供一些技术支持而已。', NOW() - INTERVAL '1 day' + INTERVAL '2 hours', NOW() - INTERVAL '1 day' + INTERVAL '2 hours'),
(5, 9, 6, '谢谢你陪我锻炼，其实你的进步也很大呢！一起加油💪', NOW() - INTERVAL '3 days' + INTERVAL '1 hour', NOW() - INTERVAL '3 days' + INTERVAL '1 hour'),
(6, 10, 5, '能陪你做你喜欢的事情，我也很开心。下次我们再去其他书店逛逛。', NOW() - INTERVAL '1 day' + INTERVAL '30 minutes', NOW() - INTERVAL '1 day' + INTERVAL '30 minutes');

-- 重置评论ID序列
SELECT setval('comments_comment_id_seq', (SELECT MAX(comment_id) FROM comments));

-- 重置用户关系ID序列
SELECT setval('user_relationships_id_seq', (SELECT MAX(id) FROM user_relationships));

-- 注意：单身用户的帖子没有评论，因为只有情侣之间可以互相评论
-- Alice (ID:1) 和 Bob (ID:2) 是单身，所以他们的帖子 (1,2,3,4) 没有评论

-- This data demonstrates the couple blog system:
-- 1. Single users (Alice, Bob) can only see their own posts
-- 2. Coupled users (Charlie&Diana, Edward&Fiona) can see each other's posts
-- 3. Only couples can comment on each other's posts
-- 4. User relationships are managed in the user_relationships table
-- 5. Each user has profile image and hobby arrays
-- 6. Relationship history is preserved with created_at timestamps

-- Test queries to verify the data:
-- Get user's partner:
-- SELECT CASE WHEN user1_id = 3 THEN user2_id WHEN user2_id = 3 THEN user1_id END as partner_id
-- FROM user_relationships WHERE (user1_id = 3 OR user2_id = 3) AND status = 'ACTIVE';

-- Get posts visible to user 3 (Charlie):
-- SELECT p.* FROM posts p WHERE p.user_id IN (
--     SELECT 3 UNION ALL 
--     SELECT CASE WHEN user1_id = 3 THEN user2_id WHEN user2_id = 3 THEN user1_id END
--     FROM user_relationships WHERE (user1_id = 3 OR user2_id = 3) AND status = 'ACTIVE'
-- ); 