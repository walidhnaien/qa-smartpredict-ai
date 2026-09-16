export interface QualityIntelligence {

  // =========================
  // SCORES
  // =========================

  coverageScore: number;
  defectScore: number;
  feedbackScore: number;
  incidentScore: number;
  rcaScore: number;

  qualityIntelligenceScore: number;


  // =========================
  // COVERAGE
  // =========================

  totalRequirements: number;
  coveredRequirements: number;
  uncoveredRequirements: number;


  // =========================
  // DEFECT
  // =========================

  totalStories: number;
  totalBugs: number;
  bugRatio: number;


  // =========================
  // INCIDENT
  // =========================

  totalIncidents: number;
  criticalClientBugs: number;
  mostImpactedClient: string;


  // =========================
  // RCA
  // =========================

  incidentsWithRca: number;

  rcaCoverage: number;

  correctiveActionCoverage: number;

  preventiveActionCoverage: number;

  closureRate: number;

  rcaDone: number;

  rcaInProgress: number;

  rcaToDo: number;
}