// Synthetic test data only.
// No production release or customer data.

import {
  DashboardQualitySnapshot,
  WhatIfExportSnapshot
} from '../quality-report-export.model';

export function createDashboardSnapshot(releaseId = 'synthetic-release'): DashboardQualitySnapshot {
  return {
    releaseId,
    capturedAt: '2026-01-01T10:00:00Z',
    finalQis: 72,
    score: {
      coverageScore: 80,
      defectScore: 30,
      feedbackScore: 80,
      incidentScore: 80,
      rcaScore: 80,
      qualityIntelligenceScore: 70,
      totalRequirements: 10,
      coveredRequirements: 8,
      uncoveredRequirements: 2,
      totalStories: 10,
      totalBugs: 7,
      bugRatio: 70,
      totalIncidents: 5,
      criticalClientBugs: 1,
      mostImpactedClient: 'Synthetic client',
      incidentsWithRca: 4,
      rcaCoverage: 80,
      correctiveActionCoverage: 80,
      preventiveActionCoverage: 80,
      closureRate: 80,
      rcaDone: 4,
      rcaInProgress: 1,
      rcaToDo: 0
    },
    sonar: {
      projectId: 1,
      baseQis: 70,
      sonarScore: 80,
      sonarWeight: 0.2,
      baseQisContribution: 56,
      sonarContribution: 16,
      finalQis: 72,
      sonarQualityGate: 'OK',
      bugs: 0,
      vulnerabilities: 0,
      codeSmells: 5,
      coverage: 80,
      duplicatedLinesDensity: 2
    }
  };
}

export function createSimulationSnapshot(releaseId = 'synthetic-release'): WhatIfExportSnapshot {
  return {
    releaseId,
    capturedAt: '2026-01-01T10:01:00Z',
    currentQis: 72,
    simulatedQis: 80,
    difference: 8,
    currentDecision: 'GO WITH RISK',
    simulatedDecision: 'GO',
    targetQis: 80,
    gapToGo: 8,
    currentValues: {
      coverage: 80,
      defect: 30,
      feedback: 80,
      incident: 80,
      rca: 80,
      sonar: 80
    },
    simulatedValues: {
      coverage: 80,
      defect: 80,
      feedback: 80,
      incident: 80,
      rca: 80,
      sonar: 80
    },
    weights: {
      coverage: 16,
      defect: 16,
      feedback: 16,
      incident: 16,
      rca: 16,
      sonar: 20
    },
    recommendation: {
      metric: 'Defect Quality',
      currentValue: 30,
      targetValue: 80,
      improvement: 50,
      projectedQis: 80
    }
  };
}
