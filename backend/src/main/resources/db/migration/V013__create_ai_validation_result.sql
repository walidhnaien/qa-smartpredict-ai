
-- QA SmartPredict AI
-- Flyway V013: AI validation audit trail
-- Additive migration: preserves existing QA data.

CREATE TABLE ai_validation_result (
    id UUID PRIMARY KEY,

    source_metrics JSONB NOT NULL,
    llama_response JSONB NOT NULL,

    java_status VARCHAR(10) NOT NULL,
    java_issues JSONB NOT NULL DEFAULT '[]'::jsonb,

    judge_status VARCHAR(10) NOT NULL,
    judge_score INTEGER,
    judge_issues JSONB NOT NULL DEFAULT '[]'::jsonb,
    judge_response JSONB,

    java_error TEXT,
    judge_error TEXT,

    llama_model VARCHAR(150) NOT NULL,
    judge_model VARCHAR(150),

    llama_duration_ms BIGINT,
    judge_duration_ms BIGINT,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ai_java_status
        CHECK (java_status IN ('PASS', 'FAIL', 'ERROR')),

    CONSTRAINT chk_ai_judge_status
        CHECK (judge_status IN (
            'PASS', 'FAIL', 'ERROR', 'NOT_RUN'
        )),

    CONSTRAINT chk_ai_judge_score
        CHECK (
            judge_score IS NULL
            OR judge_score BETWEEN 0 AND 100
        ),

    CONSTRAINT chk_ai_llama_duration
        CHECK (
            llama_duration_ms IS NULL
            OR llama_duration_ms >= 0
        ),

    CONSTRAINT chk_ai_judge_duration
        CHECK (
            judge_duration_ms IS NULL
            OR judge_duration_ms >= 0
        ),

    CONSTRAINT chk_ai_java_issues_array
        CHECK (jsonb_typeof(java_issues) = 'array'),

    CONSTRAINT chk_ai_judge_issues_array
        CHECK (jsonb_typeof(judge_issues) = 'array')
);

CREATE INDEX idx_ai_validation_result_created_at
    ON ai_validation_result (created_at DESC);

CREATE INDEX idx_ai_validation_result_judge_status
    ON ai_validation_result (judge_status);
