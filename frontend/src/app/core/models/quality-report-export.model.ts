import { AiQualityReport } from './ai-quality-report.model';
import { WorkflowStep } from './ai-orchestrator.model';
import { QualityIntelligence } from './quality-intelligence';
import { QisSonar } from './qis-sonar.model';
import { Release } from './release.model';

export interface QualityDimensionValues {
  coverage: number;
  defect: number;
  feedback: number;
  incident: number;
  rca: number;
  sonar: number | null;
}

export interface WhatIfRecommendation {
  metric: string;
  currentValue: number;
  targetValue: number;
  improvement: number;
  projectedQis: number;
}

export interface WhatIfExportSnapshot {
  releaseId: string;
  capturedAt: string;

  currentQis: number;
  simulatedQis: number;
  difference: number;

  currentDecision: string;
  simulatedDecision: string;

  targetQis: number;
  gapToGo: number;

  currentValues: QualityDimensionValues;
  simulatedValues: QualityDimensionValues;
  weights: QualityDimensionValues;

  recommendation: WhatIfRecommendation | null;
}

export interface DashboardQualitySnapshot {
  releaseId: string;
  capturedAt: string;

  score: QualityIntelligence;
  sonar: QisSonar;
  finalQis: number;
}

export interface QualityReportExportData {
  schemaVersion: '1.0';
  applicationVersion: '4.0.5';
  exportedAt: string;

  release: Release;

  historicalWorkflow: {
    report: AiQualityReport;
    steps: WorkflowStep[];
  };

  currentDashboard: DashboardQualitySnapshot | null;
  simulation: WhatIfExportSnapshot | null;
}
