export interface AiRisk {
  dimension: string;
  severity: string;
  reason: string;
}

export interface AiPriorityAction {
  priority: number;
  action: string;
  expectedImpact: string;
}

export interface AiForecast {
  predictedNextQis: number;
  trend: string;
  expectedReleaseStatus: string;
}

export interface AiGenerativeAnalysis {
  executiveSummary: string;
  releaseRationale: string;
  topRisks: AiRisk[];
  priorityActions: AiPriorityAction[];
  managerMessage: string;
  confidenceExplanation: string;
}

export interface AiIntelligenceResponse {
  qis: number;

  riskLevel: string;
  releaseDecision: string;
  confidence: number;
  mainRisk: string;

  summary: string;
  executiveSummary: string;

  strengths: string[];
  weaknesses: string[];
  recommendations: string[];

  forecast: AiForecast;

  aiAnalysis: AiGenerativeAnalysis;
}