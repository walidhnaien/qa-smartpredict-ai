
CREATE TABLE ai_review_validation_history (

    id UUID PRIMARY KEY,

    review_result_id UUID NOT NULL
        REFERENCES ai_review_result(id),

    batch_id UUID NOT NULL,

    validator_version VARCHAR(50) NOT NULL,

    validation_status VARCHAR(20) NOT NULL,

    validation_issues JSONB NOT NULL
        DEFAULT '[]'::jsonb,

    correction_status VARCHAR(20) NOT NULL
        DEFAULT 'NOT_RUN',

    deterministic_corrections JSONB NOT NULL
        DEFAULT '[]'::jsonb,

    duration_ms BIGINT,

    error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_review_history_status
        CHECK (
            validation_status IN (
                'PASS',
                'FAIL',
                'ERROR',
                'NOT_VERIFIABLE'
            )
        ),

    CONSTRAINT chk_correction_status
        CHECK (
            correction_status IN (
                'PASS',
                'FAIL',
                'NOT_RUN',
                'NOT_APPLICABLE',
                'NOT_VERIFIABLE'
            )
        ),

    CONSTRAINT chk_history_issues_array
        CHECK (
            jsonb_typeof(validation_issues) = 'array'
        ),

    CONSTRAINT chk_history_corrections_array
        CHECK (
            jsonb_typeof(deterministic_corrections) = 'array'
        ),

    CONSTRAINT chk_history_duration
        CHECK (
            duration_ms IS NULL OR duration_ms >= 0
        ),

    CONSTRAINT uq_review_validation_batch
        UNIQUE (review_result_id, batch_id)
);

CREATE INDEX idx_review_history_result
    ON ai_review_validation_history(review_result_id);

CREATE INDEX idx_review_history_batch
    ON ai_review_validation_history(batch_id);

CREATE INDEX idx_review_history_version
    ON ai_review_validation_history(validator_version);

CREATE INDEX idx_review_history_status
    ON ai_review_validation_history(validation_status);

CREATE INDEX idx_review_history_created
    ON ai_review_validation_history(created_at DESC);
