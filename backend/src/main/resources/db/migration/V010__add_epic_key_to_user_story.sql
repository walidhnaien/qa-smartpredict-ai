ALTER TABLE user_story
ADD COLUMN epic_key VARCHAR(255);

CREATE INDEX idx_user_story_epic_key
ON user_story(epic_key);