-- Rich Text Content Migration Script
-- This script adds rich text support to posts and comments tables

-- Add rich text content columns to posts table
ALTER TABLE posts ADD COLUMN IF NOT EXISTS content_rich JSONB;

-- Add rich text content columns to comments table  
ALTER TABLE comments ADD COLUMN IF NOT EXISTS content_rich JSONB;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_posts_content_rich_gin ON posts USING GIN (content_rich);
CREATE INDEX IF NOT EXISTS idx_comments_content_rich_gin ON comments USING GIN (content_rich);

-- Create partial indexes for content type searches
CREATE INDEX IF NOT EXISTS idx_posts_content_type ON posts USING GIN ((content_rich->>'type'));
CREATE INDEX IF NOT EXISTS idx_comments_content_type ON comments USING GIN ((content_rich->>'type'));

-- Function to convert plain text to rich text JSON format
CREATE OR REPLACE FUNCTION convert_to_rich_text(plain_text TEXT) 
RETURNS JSONB AS $$
BEGIN
    IF plain_text IS NULL OR plain_text = '' THEN
        RETURN jsonb_build_object(
            'type', 'plain_text',
            'version', '1.0',
            'plainText', ''
        );
    END IF;
    
    RETURN jsonb_build_object(
        'type', 'plain_text',
        'version', '1.0',
        'delta', jsonb_build_object(
            'ops', jsonb_build_array(
                jsonb_build_object('insert', plain_text || E'\n')
            )
        ),
        'plainText', plain_text
    );
END;
$$ LANGUAGE plpgsql;

-- Migrate existing posts content to rich text format
UPDATE posts 
SET content_rich = convert_to_rich_text(content)
WHERE content_rich IS NULL;

-- Migrate existing comments content to rich text format
UPDATE comments 
SET content_rich = convert_to_rich_text(content)
WHERE content_rich IS NULL;

-- Verify migration results
DO $$
DECLARE
    posts_migrated INTEGER;
    comments_migrated INTEGER;
    posts_total INTEGER;
    comments_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO posts_total FROM posts;
    SELECT COUNT(*) INTO posts_migrated FROM posts WHERE content_rich IS NOT NULL;
    
    SELECT COUNT(*) INTO comments_total FROM comments;
    SELECT COUNT(*) INTO comments_migrated FROM comments WHERE content_rich IS NOT NULL;
    
    RAISE NOTICE 'Migration completed:';
    RAISE NOTICE 'Posts: % out of % migrated (%.1f%%)', 
        posts_migrated, posts_total, 
        CASE WHEN posts_total > 0 THEN (posts_migrated::FLOAT / posts_total * 100) ELSE 0 END;
    RAISE NOTICE 'Comments: % out of % migrated (%.1f%%)', 
        comments_migrated, comments_total,
        CASE WHEN comments_total > 0 THEN (comments_migrated::FLOAT / comments_total * 100) ELSE 0 END;
END;
$$;

-- Clean up function after migration
DROP FUNCTION IF EXISTS convert_to_rich_text(TEXT);

-- Add constraints to ensure data consistency
ALTER TABLE posts ADD CONSTRAINT posts_content_rich_check 
    CHECK (content_rich IS NOT NULL AND content_rich ? 'type' AND content_rich ? 'plainText');

ALTER TABLE comments ADD CONSTRAINT comments_content_rich_check 
    CHECK (content_rich IS NOT NULL AND content_rich ? 'type' AND content_rich ? 'plainText');

-- Update existing data.sql insert statements to include rich text content
-- This ensures new test data includes both legacy and rich content

COMMENT ON COLUMN posts.content_rich IS 'Rich text content in JSON format supporting both plain text and formatted content';
COMMENT ON COLUMN comments.content_rich IS 'Rich text content in JSON format supporting both plain text and formatted content';

-- Backup original content columns (optional, can be dropped later)
COMMENT ON COLUMN posts.content IS 'Legacy plain text content - kept for backward compatibility';
COMMENT ON COLUMN comments.content IS 'Legacy plain text content - kept for backward compatibility';

-- Performance monitoring query
-- To check content type distribution:
-- SELECT 
--     content_rich->>'type' as content_type,
--     COUNT(*) as count,
--     ROUND(COUNT(*)::NUMERIC / SUM(COUNT(*)) OVER() * 100, 2) as percentage
-- FROM posts 
-- GROUP BY content_rich->>'type';

RAISE NOTICE 'Rich text migration completed successfully!';
RAISE NOTICE 'You can now start using the rich text functionality.';
RAISE NOTICE 'Legacy content fields are preserved for backward compatibility.'; 