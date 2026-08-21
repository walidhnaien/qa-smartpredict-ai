export interface QualityIntelligence {

  coverageScore: number;

  defectScore: number;

  feedbackScore: number;

  incidentScore: number;

  qualityIntelligenceScore: number;

  totalRequirements: number;
coveredRequirements: number;
uncoveredRequirements: number;

totalStories: number;
totalBugs: number;
bugRatio: number;
}