CREATE TABLE IF NOT EXISTS ai_multi_agent_revision (
    id UUID PRIMARY KEY,
    validation_id UUID NOT NULL REFERENCES ai_validation_result(id),
    revision_version VARCHAR(32) NOT NULL DEFAULT 'LLAMA_V2',
    model VARCHAR(128) NOT NULL,
    prompt_version VARCHAR(64) NOT NULL,
    input_snapshot JSONB NOT NULL,
    system_prompt TEXT NOT NULL,
    user_prompt TEXT NOT NULL,
    raw_response TEXT,
    revised_analysis JSONB,
    validation_report JSONB,
    execution_status VARCHAR(24) NOT NULL,
    error_message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_multi_agent_revision_validation_created
    ON ai_multi_agent_revision(validation_id, created_at DESC);
