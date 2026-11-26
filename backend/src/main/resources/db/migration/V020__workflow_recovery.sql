ALTER TABLE ai_workflow_run ADD COLUMN recovery_count INTEGER NOT NULL DEFAULT 0 CHECK (recovery_count >= 0);
