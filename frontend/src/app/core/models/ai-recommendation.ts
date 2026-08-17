export interface AiRecommendation {

  riskLevel: string;

  summary: string;

  executiveSummary: string;

  sprintDecision: string;

  strengths: string[];

  weaknesses: string[];

  recommendations: string[];
}