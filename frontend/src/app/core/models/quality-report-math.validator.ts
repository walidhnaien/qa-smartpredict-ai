import {
  DashboardQualitySnapshot,
  WhatIfExportSnapshot
} from './quality-report-export.model';

const EPSILON = 0.000001;
const JAVA_TOLERANCE = 0.02;

const KEYS = [
  'coverage',
  'defect',
  'feedback',
  'incident',
  'rca',
  'sonar'
] as const;

const LABELS = [
  'Coverage',
  'Defect Quality',
  'Feedback',
  'Incident',
  'RCA',
  'Sonar Quality'
];

function validScore(value: unknown): value is number {
  return typeof value === 'number' &&
    Number.isFinite(value) &&
    value >= 0 &&
    value <= 100;
}

function close(
  actual: number,
  expected: number,
  tolerance = EPSILON
): boolean {
  return Number.isFinite(actual) &&
    Number.isFinite(expected) &&
    Math.abs(actual - expected) <= tolerance + EPSILON;
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

function decision(score: number): string {
  if (score >= 80) return 'GO';
  if (score >= 60) return 'GO WITH RISK';
  return 'NO GO';
}

export function validateDashboardMath(
  dashboard: DashboardQualitySnapshot
): string[] {

  const errors: string[] = [];
  const score = dashboard.score;
  const sonar = dashboard.sonar;

  if (!score || !sonar) {
    return ['Dashboard metrics are incomplete.'];
  }

  const scores = [
    score.coverageScore,
    score.defectScore,
    score.feedbackScore,
    score.incidentScore,
    score.rcaScore,
    score.qualityIntelligenceScore,
    dashboard.finalQis,
    sonar.baseQis,
    sonar.finalQis,
    sonar.baseQisContribution,
    sonar.sonarContribution
  ];

  if (!scores.every(validScore)) {
    errors.push('Invalid dashboard quality scores.');
    return errors;
  }

  const weight = sonar.sonarWeight;

  if (
    !Number.isFinite(weight) ||
    weight < 0 ||
    weight > 1
  ) {
    errors.push('Invalid backend Sonar weight.');
    return errors;
  }

  if (!close(dashboard.finalQis, sonar.finalQis)) {
    errors.push('Dashboard final QIS mismatch.');
  }

  if (!close(
    score.qualityIntelligenceScore,
    sonar.baseQis,
    JAVA_TOLERANCE
  )) {
    errors.push('Business QIS source mismatch.');
  }

  if (sonar.sonarScore === null) {

    if (
      !close(weight, 0) ||
      !close(sonar.sonarContribution, 0) ||
      !close(sonar.baseQisContribution, sonar.baseQis) ||
      !close(sonar.finalQis, sonar.baseQis)
    ) {
      errors.push('Invalid missing Sonar fallback.');
    }

    return errors;
  }

  if (!validScore(sonar.sonarScore)) {
    errors.push('Invalid Sonar score.');
    return errors;
  }

  const expectedBase =
    sonar.baseQis * (1 - weight);

  const expectedSonar =
    sonar.sonarScore * weight;

  if (!close(
    sonar.baseQisContribution,
    expectedBase,
    JAVA_TOLERANCE
  )) {
    errors.push('Invalid business QIS contribution.');
  }

  if (!close(
    sonar.sonarContribution,
    expectedSonar,
    JAVA_TOLERANCE
  )) {
    errors.push('Invalid Sonar contribution.');
  }

  if (!close(
    sonar.finalQis,
    expectedBase + expectedSonar,
    JAVA_TOLERANCE
  )) {
    errors.push('Invalid backend final QIS calculation.');
  }

  if (!close(
    sonar.finalQis,
    sonar.baseQisContribution +
      sonar.sonarContribution,
    JAVA_TOLERANCE
  )) {
    errors.push('Backend contribution total mismatch.');
  }

  return errors;
}

export function validateSimulationMath(
  simulation: WhatIfExportSnapshot,
  dashboard: DashboardQualitySnapshot | null
): string[] {

  const errors: string[] = [];

  if (
    !simulation.currentValues ||
    !simulation.simulatedValues ||
    !simulation.weights
  ) {
    return ['Simulation dimensions are incomplete.'];
  }

  const current = KEYS.map(
    key => simulation.currentValues[key]
  );

  const simulated = KEYS.map(
    key => simulation.simulatedValues[key]
  );

  const weights = KEYS.map(
    key => simulation.weights[key]
  );

  if (
    !current.every(validScore) ||
    !simulated.every(validScore) ||
    !weights.every(validScore)
  ) {
    return ['Invalid simulation scores or weights.'];
  }

  if (
    !validScore(simulation.currentQis) ||
    !validScore(simulation.simulatedQis)
  ) {
    return ['Invalid simulation QIS values.'];
  }

  const totalWeight =
    weights.reduce((sum, value) => sum + value, 0);

  if (!close(totalWeight, 100)) {
    errors.push('Simulation weights must total 100%.');
  }

  const weightedScore = (values: number[]): number =>
    values.reduce(
      (sum, value, index) =>
        sum + value * weights[index] / 100,
      0
    );

  if (!close(
    simulation.currentQis,
    weightedScore(current),
    JAVA_TOLERANCE
  )) {
    errors.push('Simulation baseline QIS mismatch.');
  }

  const expectedSimulated =
    round2(weightedScore(simulated));

  if (!close(
    simulation.simulatedQis,
    expectedSimulated
  )) {
    errors.push('Invalid simulated QIS calculation.');
  }

  const rawDifference =
    simulation.simulatedQis - simulation.currentQis;

  const expectedDifference =
    Math.abs(rawDifference) < 0.02
      ? 0
      : round2(rawDifference);

  if (!close(
    simulation.difference,
    expectedDifference
  )) {
    errors.push('Invalid simulation QIS difference.');
  }

  if (
    simulation.currentDecision !==
      decision(simulation.currentQis) ||
    simulation.simulatedDecision !==
      decision(simulation.simulatedQis)
  ) {
    errors.push('Invalid simulation decision.');
  }

  const expectedGap = Math.max(
    0,
    round2(80 - simulation.currentQis)
  );

  if (
    !close(simulation.targetQis, 80) ||
    !close(simulation.gapToGo, expectedGap)
  ) {
    errors.push('Invalid simulation target or gap.');
  }

  const candidates = current
    .map((value, index) => ({
      metric: LABELS[index],
      currentValue: value,
      improvement: weights[index] > 0
        ? expectedGap / (weights[index] / 100)
        : Infinity
    }))
    .map(candidate => ({
      ...candidate,
      targetValue:
        candidate.currentValue + candidate.improvement
    }))
    .filter(candidate => candidate.targetValue <= 100)
    .sort((a, b) => a.improvement - b.improvement);

  const expectedRecommendation =
    simulation.currentQis >= 80
      ? null
      : candidates[0] ?? null;

  const actual = simulation.recommendation;

  if (!expectedRecommendation) {

    if (actual !== null) {
      errors.push('Unexpected simulation recommendation.');
    }

  } else if (
    !actual ||
    actual.metric !== expectedRecommendation.metric ||
    !close(
      actual.currentValue,
      expectedRecommendation.currentValue
    ) ||
    !close(
      actual.targetValue,
      Math.ceil(
        expectedRecommendation.targetValue * 100
      ) / 100
    ) ||
    !close(
      actual.improvement,
      round2(expectedRecommendation.improvement)
    ) ||
    !close(actual.projectedQis, 80)
  ) {
    errors.push('Invalid simulation recommendation.');
  }

  if (!dashboard) {
    errors.push('Simulation requires a dashboard snapshot.');
    return errors;
  }

  if (!dashboard.score || !dashboard.sonar) {
    errors.push('Simulation dashboard is incomplete.');
    return errors;
  }

  if (simulation.releaseId !== dashboard.releaseId) {
    errors.push('Simulation dashboard release mismatch.');
  }

  if (!close(
    simulation.currentQis,
    dashboard.finalQis
  )) {
    errors.push('Simulation dashboard QIS mismatch.');
  }

  const expectedValues = [
    dashboard.score.coverageScore,
    dashboard.score.defectScore,
    dashboard.score.feedbackScore,
    dashboard.score.incidentScore,
    dashboard.score.rcaScore,
    dashboard.sonar.sonarScore
  ];

  if (
    expectedValues.some(
      (value, index) =>
        !validScore(value) ||
        !close(current[index], value)
    )
  ) {
    errors.push('Simulation dashboard KPI mismatch.');
  }

  if (!close(
    weights[5],
    dashboard.sonar.sonarWeight * 100
  )) {
    errors.push('Simulation backend Sonar weight mismatch.');
  }

  const businessWeight =
    weights.slice(0, 5)
      .reduce((sum, value) => sum + value, 0);

  if (businessWeight > EPSILON) {

    const businessQis = current
      .slice(0, 5)
      .reduce(
        (sum, value, index) =>
          sum + value * weights[index],
        0
      ) / businessWeight;

    if (!close(
      round2(businessQis),
      dashboard.sonar.baseQis,
      JAVA_TOLERANCE
    )) {
      errors.push('Simulation business QIS mismatch.');
    }
  }

  return errors;
}
