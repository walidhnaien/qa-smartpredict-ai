-- =====================================================
-- QA SmartPredict AI
-- V001__init_schema.sql
-- Initial Database Schema
-- =====================================================

CREATE TABLE application (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE release (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,
    version VARCHAR(100) NOT NULL,

    status VARCHAR(50),

    start_date DATE,
    end_date DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_release_application
        FOREIGN KEY (application_id)
        REFERENCES application(id)
);

CREATE TABLE requirement (
    id UUID PRIMARY KEY,

    application_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL UNIQUE,

    title VARCHAR(255) NOT NULL,

    description TEXT,

    criticality VARCHAR(50),

    status VARCHAR(50),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_requirement_application
        FOREIGN KEY (application_id)
        REFERENCES application(id)
);

CREATE TABLE user_story (
    id UUID PRIMARY KEY,

    release_id UUID NOT NULL,

    jira_key VARCHAR(50) NOT NULL UNIQUE,

    summary VARCHAR(500),

    description TEXT,

    status VARCHAR(100),

    priority VARCHAR(100),

    sprint VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_story_release
        FOREIGN KEY (release_id)
        REFERENCES release(id)
);

CREATE TABLE requirement_user_story (

    requirement_id UUID NOT NULL,

    user_story_id UUID NOT NULL,

    PRIMARY KEY (
        requirement_id,
        user_story_id
    ),

    CONSTRAINT fk_rus_requirement
        FOREIGN KEY (requirement_id)
        REFERENCES requirement(id),

    CONSTRAINT fk_rus_user_story
        FOREIGN KEY (user_story_id)
        REFERENCES user_story(id)
);

CREATE TABLE import_job (

    id UUID PRIMARY KEY,

    source_system VARCHAR(50),

    file_name VARCHAR(500),

    status VARCHAR(50),

    imported_records INTEGER DEFAULT 0,

    rejected_records INTEGER DEFAULT 0,

    import_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    error_message TEXT
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_requirement_code
ON requirement(code);

CREATE INDEX idx_user_story_jira_key
ON user_story(jira_key);

CREATE INDEX idx_release_version
ON release(version);