CREATE TABLE ai_workflow_run (
 id UUID PRIMARY KEY, release_id UUID NOT NULL REFERENCES release(id),
 workflow_version VARCHAR(20) NOT NULL, mode VARCHAR(20) NOT NULL CHECK (mode='SIMULATION'),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED')),
 current_step VARCHAR(50), created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, duration_ms BIGINT, error_message TEXT,
 validation_id UUID, batch_id UUID, revision_id UUID
);
CREATE UNIQUE INDEX uq_ai_workflow_active ON ai_workflow_run ((1)) WHERE status IN ('PENDING','RUNNING');
CREATE TABLE ai_workflow_step (
 id UUID PRIMARY KEY, run_id UUID NOT NULL REFERENCES ai_workflow_run(id),
 step_name VARCHAR(50) NOT NULL, step_order INTEGER NOT NULL CHECK(step_order BETWEEN 1 AND 8),
 status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','RUNNING','SIMULATED','FAILED','SKIPPED')),
 model VARCHAR(80), attempt INTEGER NOT NULL DEFAULT 0, started_at TIMESTAMPTZ,
 completed_at TIMESTAMPTZ, duration_ms BIGINT, error_message TEXT,
 result_reference JSONB, UNIQUE(run_id,step_order)
);
