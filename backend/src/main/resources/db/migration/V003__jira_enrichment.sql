ALTER TABLE user_story
ADD COLUMN issue_type VARCHAR(50);

ALTER TABLE user_story
ADD COLUMN reporter VARCHAR(255);

ALTER TABLE user_story
ADD COLUMN assignee VARCHAR(255);

ALTER TABLE user_story
ADD COLUMN created_date TIMESTAMP;

ALTER TABLE user_story
ADD COLUMN resolved_date TIMESTAMP;