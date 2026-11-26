export type RunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
export interface WorkflowRun { id: string; release_id: string; mode: string; status: RunStatus; current_step: string | null; recovery_count: number; duration_ms: number | null; error_message: string | null; }
export interface WorkflowStep { step_name: string; step_order: number; status: string; model: string | null; attempt: number; duration_ms: number | null; error_message: string | null; }
export interface StartResponse { runId: string; status: RunStatus; }
export interface Preflight { status: string; readyToRun: boolean; warnings: string[]; blockers: string[]; }
export const STEP_NAMES = ['COLLECT_METRICS','INITIAL_ANALYSIS','EXTRACT_CLAIMS','GOVERNANCE_REVIEW','METRIC_REVIEW','CONSOLIDATE','CONTROLLED_REVISION','FINAL_VALIDATION'];
export const active = (run: WorkflowRun | null) => run?.status === 'PENDING' || run?.status === 'RUNNING';
