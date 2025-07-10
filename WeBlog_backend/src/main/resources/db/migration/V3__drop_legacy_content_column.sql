-- V3: Drop legacy content column from posts and comments tables
-- 
-- Since we have successfully migrated all content to the content_rich JSONb field,
-- we can now safely remove the legacy content columns to simplify the schema.

-- Drop the content column from posts table
ALTER TABLE posts DROP COLUMN IF EXISTS content;

-- Drop the content column from comments table  
ALTER TABLE comments DROP COLUMN IF EXISTS content;

-- Log the completion
DO $$
BEGIN
    RAISE NOTICE 'Successfully dropped legacy content columns from posts and comments tables';
END $$; 