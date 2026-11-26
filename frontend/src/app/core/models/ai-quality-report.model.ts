import { WorkflowRun } from './ai-orchestrator.model';

export interface QualityReportRun
  extends Omit<WorkflowRun, 'recovery_count'> {
  recovery_count?: number | null;
  workflow_version?: string;
  created_at?: string | null;
  started_at?: string | null;
  completed_at?: string | null;
  validation_id?: string | null;
  batch_id?: string | null;
  revision_id?: string | null;
}

export interface QualityRisk {
  dimension?: string | null;
  severity?: string | null;
  reason?: string | null;
}

export interface PriorityAction {
  priority?: number | null;
  action?: string | null;
  origin?: string | null;
  evidence: unknown;
  expectedImpact?: string | null;
  acceptanceCriterion?: string | null;
}

export interface SemanticAssessment {
  premise: string;
  evidence: string;
  relation: string;
  claimType: string;
  hypothesis: string;
  sourcePath: string;
  referenceId: string;
  statementId: string;
  derivedClaim: boolean;
  findingNature: string;
  semanticStatus: string;
  parentStatementId?: string;
}

export interface SemanticAlert {
  before: string | null;
  after: string | null;
  model?: string | null;
  status?: string | null;
  sourceFieldChanged?: boolean | null;
  assessment?: SemanticAssessment | null;
}

export interface SemanticTraceability {
  status?: string | null;
  alertCount?: number | null;
  evidenceGaps?: number | null;
  evaluatedClaims?: number | null;
  uniqueAlertedFields?: number | null;
  changedAlertedFields?: number | null;
  potentialContradictions?: number | null;
  confirmedSemanticIssues: number | null;
  confirmedResolvedSemanticIssues: number | null;
  semanticIssueResolutionRate: number | null;
  alerts?: SemanticAlert[] | null;
}

export interface QualityValidationReport {
  status?: string | null;
  note?: string | null;
  detectedIssues?: number | null;
  resolvedIssues?: number | null;
  issues?: unknown[] | null;
  issueResolution?: unknown[] | null;
  issueResolutionRate: number | null;
  humanReviewRequired?: boolean | null;
  semanticQualityCertified?: boolean | null;
  riskWordingImproved?: boolean;
  semanticTraceability?: SemanticTraceability | null;
}

export interface RevisedQualityAnalysis {
  qis?: number | null;
  bugRatio?: number | null;
  mainRisk?: string | null;
  riskLevel?: string | null;
  confidence?: number | null;
  defectScore?: number | null;
  releaseDecision?: string | null;
  executiveSummary?: string | null;
  releaseRationale?: string | null;
  confidenceExplanation?: string | null;
  managerMessage?: string | null;
  topRisks?: QualityRisk[] | null;
  priorityActions?: PriorityAction[] | null;
  aiAdvisory?: {
    status: string;
    questions: unknown[];
  };
  revision?: unknown;
}

export interface AiQualityReport {
  run: QualityReportRun;
  humanReviewRequired?: boolean | null;
  semanticQualityCertified?: boolean | null;
  validationReport?: QualityValidationReport | null;
  revisedAnalysis?: RevisedQualityAnalysis | null;
}

export type QualityReportState =
  | 'IDLE'
  | 'LOADING'
  | 'AVAILABLE'
  | 'UNAVAILABLE'
  | 'ERROR';
