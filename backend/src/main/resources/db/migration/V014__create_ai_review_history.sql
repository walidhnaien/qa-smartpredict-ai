CREATE TABLE ai_review_test_case (
    id UUID PRIMARY KEY,
    test_case_key VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    source_of_truth JSONB NOT NULL,
    candidate JSONB NOT NULL,
    expected_result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE ai_review_result (
    id UUID PRIMARY KEY,

    validation_id UUID
        REFERENCES ai_validation_result(id),

    test_case_id UUID
        REFERENCES ai_review_test_case(id),

    -- Reviewer identification
    reviewer_model VARCHAR(200) NOT NULL,
    reviewer_provider VARCHAR(100) NOT NULL,
    model_digest VARCHAR(200),

    -- Experiment configuration
    prompt_version VARCHAR(100),
    output_mode VARCHAR(50) NOT NULL,
    system_prompt TEXT,
    user_prompt TEXT,
    request_payload JSONB,

    -- Original reviewer output
    raw_response TEXT,
    response_metadata JSONB,
    parsed_response JSONB,

    -- Reviewer verdict
    review_status VARCHAR(50),
    review_score INTEGER,
    review_issues JSONB,
    review_explanation TEXT,

    -- Deterministic Java validation
    validation_status VARCHAR(20) NOT NULL
        DEFAULT 'NOT_RUN',

    validation_issues JSONB NOT NULL
        DEFAULT '[]'::jsonb,

    -- Qualification result
    test_status VARCHAR(20) NOT NULL
        DEFAULT 'NOT_RUN',

    expected_issue_count INTEGER,
    detected_issue_count INTEGER,
    correct_correction_count INTEGER,
    false_positive_count INTEGER,

    -- Performance metrics
    prompt_tokens INTEGER,
    completion_tokens INTEGER,

    total_tokens INTEGER GENERATED ALWAYS AS
        (prompt_tokens + completion_tokens) STORED,

    total_duration_ms BIGINT,
    load_duration_ms BIGINT,
    prompt_eval_duration_ms BIGINT,
    eval_duration_ms BIGINT,

    -- Technical execution
    execution_status VARCHAR(20) NOT NULL,
    error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_review_parent
        CHECK (
            (validation_id IS NOT NULL
                AND test_case_id IS NULL)
            OR
            (validation_id IS NULL
                AND test_case_id IS NOT NULL)
        ),

    CONSTRAINT chk_review_score
        CHECK (
            review_score IS NULL
            OR review_score BETWEEN 0 AND 100
        ),

    CONSTRAINT chk_review_validation_status
        CHECK (
            validation_status IN (
                'PASS', 'FAIL', 'ERROR', 'NOT_RUN'
            )
        ),

    CONSTRAINT chk_review_test_status
        CHECK (
            test_status IN (
                'PASS', 'FAIL', 'ERROR', 'NOT_RUN'
            )
        ),

    CONSTRAINT chk_review_execution_status
        CHECK (
            execution_status IN ('SUCCESS', 'ERROR')
        ),

    CONSTRAINT chk_review_output_mode
        CHECK (
            output_mode IN (
                'TEXT',
                'JSON_MODE',
                'STRUCTURED_OUTPUT'
            )
        ),

    CONSTRAINT chk_review_validation_issues
        CHECK (
            jsonb_typeof(validation_issues) = 'array'
        )
);


CREATE INDEX idx_ai_review_validation_id
    ON ai_review_result(validation_id);

CREATE INDEX idx_ai_review_test_case_id
    ON ai_review_result(test_case_id);

CREATE INDEX idx_ai_review_model
    ON ai_review_result(reviewer_model);

CREATE INDEX idx_ai_review_created_at
    ON ai_review_result(created_at DESC);

CREATE INDEX idx_ai_review_validation_status
    ON ai_review_result(validation_status);