import { describe, it, expect } from 'vitest';

import {
  validateDashboardMath,
  validateSimulationMath
} from './quality-report-math.validator';

import {
  createDashboardSnapshot as dashboard,
  createSimulationSnapshot as simulation
} from './testing/quality-report-export.fixture';

describe('QualityReportMathValidator', () => {

  it('MATH-01 accepts coherent dashboard', () => {
    expect(validateDashboardMath(dashboard()))
      .toEqual([]);
  });

  it('MATH-02 rejects out-of-range KPI', () => {
    const d = dashboard();
    d.score.coverageScore = 101;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-03 rejects non-finite KPI', () => {
    const d = dashboard();
    d.score.defectScore = NaN;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-04 rejects invalid backend weight', () => {
    const d = dashboard();
    d.sonar.sonarWeight = 20;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-05 rejects invalid contribution', () => {
    const d = dashboard();
    d.sonar.sonarContribution = 20;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-06 accepts missing Sonar fallback', () => {
    const d = dashboard();

    Object.assign(d.sonar, {
      sonarScore: null,
      sonarWeight: 0,
      sonarContribution: 0,
      baseQisContribution: 70,
      finalQis: 70,
      sonarQualityGate: 'NOT_AVAILABLE'
    });

    d.finalQis = 70;

    expect(validateDashboardMath(d)).toEqual([]);
  });

  it('MATH-07 rejects invalid missing Sonar fallback', () => {
    const d = dashboard();
    d.sonar.sonarScore = null;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-08 accepts legitimate zero Sonar score', () => {
    const d = dashboard();

    Object.assign(d.sonar, {
      sonarScore: 0,
      sonarContribution: 0,
      finalQis: 56
    });

    d.finalQis = 56;

    expect(validateDashboardMath(d)).toEqual([]);
  });

  it('MATH-09 accepts small Java rounding difference', () => {
    const d = dashboard();
    d.sonar.sonarContribution = 16.01;
    expect(validateDashboardMath(d)).toEqual([]);
  });

  it('MATH-10 rejects inconsistent final QIS', () => {
    const d = dashboard();
    d.finalQis = 80;
    expect(validateDashboardMath(d).length)
      .toBeGreaterThan(0);
  });

  it('MATH-11 accepts coherent simulation', () => {
    expect(
      validateSimulationMath(simulation(), dashboard())
    ).toEqual([]);
  });

  it('MATH-12 rejects invalid weight total', () => {
    const s = simulation();
    s.weights.defect = 25;
    expect(validateSimulationMath(s, dashboard()).length)
      .toBeGreaterThan(0);
  });

  it('MATH-13 rejects negative simulated KPI', () => {
    const s = simulation();
    s.simulatedValues.defect = -1;
    expect(validateSimulationMath(s, dashboard()).length)
      .toBeGreaterThan(0);
  });

  it('MATH-14 rejects incorrect simulated QIS', () => {
    const s = simulation();
    s.simulatedQis = 95;
    expect(validateSimulationMath(s, dashboard()).length)
      .toBeGreaterThan(0);
  });

  it('MATH-15 rejects incorrect difference', () => {
    const s = simulation();
    s.difference = 20;
    expect(validateSimulationMath(s, dashboard()))
      .toContain('Invalid simulation QIS difference.');
  });

  it('MATH-16 rejects incorrect decision', () => {
    const s = simulation();
    s.simulatedDecision = 'NO GO';
    expect(validateSimulationMath(s, dashboard()))
      .toContain('Invalid simulation decision.');
  });

  it('MATH-17 rejects incorrect gap', () => {
    const s = simulation();
    s.gapToGo = 0;
    expect(validateSimulationMath(s, dashboard()))
      .toContain('Invalid simulation target or gap.');
  });

  it('MATH-18 rejects incorrect recommendation', () => {
    const s = simulation();
    s.recommendation!.targetValue = 90;
    expect(validateSimulationMath(s, dashboard()))
      .toContain('Invalid simulation recommendation.');
  });

  it('MATH-19 rejects missing feasible recommendation', () => {
    const s = simulation();
    s.recommendation = null;
    expect(validateSimulationMath(s, dashboard()))
      .toContain('Invalid simulation recommendation.');
  });

  it('MATH-20 rejects dashboard KPI mismatch', () => {
    const d = dashboard();
    d.score.coverageScore = 90;
    expect(validateSimulationMath(simulation(), d))
      .toContain('Simulation dashboard KPI mismatch.');
  });

  it('MATH-21 rejects different release', () => {
    const d = dashboard();
    d.releaseId = 'another-release';
    expect(validateSimulationMath(simulation(), d))
      .toContain('Simulation dashboard release mismatch.');
  });

  it('MATH-22 rejects missing dashboard', () => {
    expect(validateSimulationMath(simulation(), null))
      .toContain('Simulation requires a dashboard snapshot.');
  });

  it('MATH-23 rejects different backend Sonar weight', () => {
    const d = dashboard();
    d.sonar.sonarWeight = 0.3;
    expect(validateSimulationMath(simulation(), d))
      .toContain(
        'Simulation backend Sonar weight mismatch.'
      );
  });

  it('MATH-24 rejects unavailable simulation Sonar', () => {
    const s = simulation();
    s.currentValues.sonar = null;
    expect(validateSimulationMath(s, dashboard()).length)
      .toBeGreaterThan(0);
  });

  it('MATH-25 does not mutate source snapshots', () => {
    const d = dashboard();
    const s = simulation();

    const beforeD = structuredClone(d);
    const beforeS = structuredClone(s);

    validateDashboardMath(d);
    validateSimulationMath(s, d);

    expect(d).toEqual(beforeD);
    expect(s).toEqual(beforeS);
  });

});
