-- ============================================================
-- V008 - Relation Many-to-Many UserStory <-> Release
-- ============================================================

-- 1. Création de la table de jointure
CREATE TABLE user_story_release (
    user_story_id UUID NOT NULL,
    release_id UUID NOT NULL,

    CONSTRAINT pk_user_story_release
        PRIMARY KEY (user_story_id, release_id),

    CONSTRAINT fk_user_story_release_user_story
        FOREIGN KEY (user_story_id)
        REFERENCES user_story(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_story_release_release
        FOREIGN KEY (release_id)
        REFERENCES release(id)
        ON DELETE CASCADE
);

-- 2. Migration des associations existantes
-- user_story.release_id -> user_story_release
INSERT INTO user_story_release (user_story_id, release_id)
SELECT id, release_id
FROM user_story
WHERE release_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 3. Index pour les recherches par release
CREATE INDEX idx_user_story_release_release_id
    ON user_story_release(release_id);

-- 4. Index pour les recherches par User Story
CREATE INDEX idx_user_story_release_user_story_id
    ON user_story_release(user_story_id);