ALTER TABLE ai_workflow_run DROP CONSTRAINT ai_workflow_run_mode_check;
ALTER TABLE ai_workflow_run ADD CONSTRAINT ai_workflow_run_mode_check CHECK (mode IN ('SIMULATION','REAL'));
ALTER TABLE ai_workflow_step DROP CONSTRAINT ai_workflow_step_status_check;
ALTER TABLE ai_workflow_step ADD CONSTRAINT ai_workflow_step_status_check CHECK (status IN ('PENDING','RUNNING','SIMULATED','SUCCESS','FAILED','SKIPPED'));
