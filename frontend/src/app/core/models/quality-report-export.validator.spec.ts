import {
  createDashboardSnapshot,
  createSimulationSnapshot
} from './testing/quality-report-export.fixture';

import { describe, it, expect } from 'vitest';

import {
  validateQualityReportExport,
  ExportValidationContext
} from './quality-report-export.validator';

import {
  QualityReportExportData
} from './quality-report-export.model';

import { AiQualityReport }
  from './ai-quality-report.model';

import { WorkflowStep }
  from './ai-orchestrator.model';

import fixture
  from './testing/quality-report.fixture.json';

describe('QualityReportExportValidator', () => {

  const runId = fixture.run.id;
  const releaseId = fixture.run.release_id;

  function data(): QualityReportExportData {

    return {
      schemaVersion: '1.0',
      applicationVersion: '4.0.5',
      exportedAt: '2026-01-01T11:00:00Z',

      release: {
        id: releaseId,
        name: 'Synthetic Release',
        version: '202601.0.0-TEST',
        type: 'MAJOR',
        status: 'READY_FOR_DECISION'
      },

      historicalWorkflow: {
        report: structuredClone(fixture) as unknown as AiQualityReport,
        steps: []
      },

      currentDashboard: null,
      simulation: null
    };
  }

  function context(): ExportValidationContext {

    return {
      expectedRunId: runId,
      expectedReleaseId: releaseId,
      stepsSourceRunId: runId
    };
  }

  function step(order = 1): WorkflowStep {

    return {
      step_order: order,
      step_name: 'COLLECT_METRICS',
      status: 'SUCCESS',
      model: 'test-model',
      attempt: 1,
      duration_ms: 1000,
      error_message: null
    } as WorkflowStep;
  }

  function dashboard() {
    return createDashboardSnapshot(releaseId);
  }

  function simulation() {
    return createSimulationSnapshot(releaseId);
  }

  it('EXP-01 accepts a valid historical report', () => {

    const result =
      validateQualityReportExport(data(), context());

    expect(result.status).toBe('VALID');
  });

  it('EXP-02 rejects an unexpected run ID', () => {

    const ctx = context();
    ctx.expectedRunId = 'another-run';

    expect(
      validateQualityReportExport(data(), ctx).status
    ).toBe('INVALID');
  });

  it('EXP-03 rejects a different selected release', () => {

    const input = data();
    input.release.id = 'another-release';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-04 rejects a report from another release', () => {

    const input = data();

    input.historicalWorkflow.report.run.release_id =
      'another-release';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-05 rejects a running workflow', () => {

    const input = data();

    input.historicalWorkflow.report.run.status =
      'RUNNING';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-06 rejects a simulation workflow', () => {

    const input = data();

    Object.assign(
      input.historicalWorkflow.report.run,
      { mode: 'SIMULATION' }
    );

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-07 rejects missing revised analysis', () => {

    const input = data();

    input.historicalWorkflow.report.revisedAnalysis =
      null;

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-08 rejects workflow steps from another run', () => {

    const input = data();
    input.historicalWorkflow.steps = [step()];

    const ctx = context();
    ctx.stepsSourceRunId = 'another-run';

    expect(
      validateQualityReportExport(input, ctx).status
    ).toBe('INVALID');
  });

  it('EXP-09 rejects duplicate step orders', () => {

    const input = data();
    input.historicalWorkflow.steps = [step(), step()];

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-10 accepts correctly associated steps', () => {

    const input = data();
    input.historicalWorkflow.steps = [step()];

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('VALID');
  });

  it('EXP-11 rejects dashboard release mismatch', () => {

    const input = data();
    input.currentDashboard = dashboard();
    input.currentDashboard.releaseId = 'another-release';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-12 rejects simulation release mismatch', () => {

    const input = data();
    input.simulation = simulation();
    input.simulation.releaseId = 'another-release';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-13 rejects invalid export date', () => {

    const input = data();
    input.exportedAt = 'invalid-date';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-14 rejects invalid dashboard capture date', () => {

    const input = data();
    input.currentDashboard = dashboard();
    input.currentDashboard.capturedAt = 'invalid-date';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-15 rejects invalid simulation capture date', () => {

    const input = data();
    input.simulation = simulation();
    input.simulation.capturedAt = 'invalid-date';

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-16 warns about unavailable optional data', () => {

    const result =
      validateQualityReportExport(data(), context());

    expect(result.status).toBe('VALID');

    expect(result.warnings).toContain(
      'Current dashboard unavailable.'
    );

    expect(result.warnings).toContain(
      'What-if simulation unavailable.'
    );

    expect(result.warnings).toContain(
      'Workflow steps unavailable.'
    );
  });

  it('EXP-17 accepts matching source identifiers', () => {

    const input = data();

    input.currentDashboard = dashboard();
    input.simulation = simulation();
    input.historicalWorkflow.steps = [step()];

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('VALID');
    expect(result.warnings).toEqual([]);
  });

  it('EXP-18 rejects non-finite simulation metrics', () => {

    const input = data();
    input.simulation = simulation();
    input.simulation.simulatedQis = NaN;

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-19 does not mutate the source data', () => {

    const input = data();
    const before = structuredClone(input);

    validateQualityReportExport(input, context());

    expect(input).toEqual(before);
  });

  it('EXP-20 rejects inconsistent governance flags', () => {

    const input = data();

    input.historicalWorkflow.report
      .semanticQualityCertified = true;

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('INVALID');
  });

  it('EXP-21 accepts coherent dashboard and simulation', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.simulation = simulation();

    expect(validateQualityReportExport(input, context()).status)
      .toBe('VALID');
  });

  it('EXP-22 rejects incorrect dashboard contribution', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.currentDashboard.sonar.sonarContribution = 30;

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors).toContain('Invalid Sonar contribution.');
    }
  });

  it('EXP-23 rejects incorrect simulated QIS', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.simulation = simulation();
    input.simulation.simulatedQis = 95;

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Invalid simulated QIS calculation.');
    }
  });

  it('EXP-24 rejects simulation without dashboard', () => {
    const input = data();
    input.simulation = simulation();

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Simulation requires a dashboard snapshot.');
    }
  });

  it('EXP-25 preserves different historical and current QIS', () => {
    const input = data();
    input.currentDashboard = dashboard();

    expect(input.historicalWorkflow.report.revisedAnalysis?.qis)
      .toBe(68.89);

    expect(input.currentDashboard.finalQis).toBe(72);

    expect(validateQualityReportExport(input, context()).status)
      .toBe('VALID');
  });

  it('EXP-26 accepts valid missing Sonar fallback', () => {
    const input = data();
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
    input.currentDashboard = d;

    expect(validateQualityReportExport(input, context()).status)
      .toBe('VALID');
  });

  it('EXP-27 rejects invalid simulation weights', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.simulation = simulation();
    input.simulation.weights.defect = 25;

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Simulation weights must total 100%.');
    }
  });

  it('EXP-28 rejects incorrect simulation recommendation', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.simulation = simulation();
    input.simulation.recommendation!.targetValue = 95;

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Invalid simulation recommendation.');
    }
  });

  it('EXP-29 preserves all coherent source snapshots', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.simulation = simulation();

    const before = structuredClone(input);

    expect(validateQualityReportExport(input, context()).status)
      .toBe('VALID');

    expect(input).toEqual(before);
  });

  it('EXP-30 rejects incomplete dashboard metrics', () => {
    const input = data();
    input.currentDashboard = dashboard();
    input.currentDashboard.score.coverageScore = NaN;

    const result = validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Invalid dashboard quality scores.');
    }
  });


  // Governance integration

  it('EXP-31 accepts a valid governance contract', () => {
    const input = data();

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('VALID');
  });

  it('EXP-32 rejects disabled human review at both levels', () => {
    const input = data();
    const report = input.historicalWorkflow.report;

    report.humanReviewRequired = false;
    report.validationReport!.humanReviewRequired = false;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Human review must remain required.');
    }
  });

  it('EXP-33 rejects semantic certification at both levels', () => {
    const input = data();
    const report = input.historicalWorkflow.report;

    report.semanticQualityCertified = true;
    report.validationReport!.semanticQualityCertified = true;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Semantic quality must not be certified.');
    }
  });

  it('EXP-34 rejects unsupported governance versions', () => {
    const input = data();

    input.historicalWorkflow.report.run.workflow_version =
      'V9.0.0';

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Unsupported workflow governance contract.');
    }
  });

  it('EXP-35 rejects invalid guardrails status', () => {
    const input = data();

    input.historicalWorkflow.report
      .validationReport!.status = 'PASS';

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Invalid final guardrails status.');
    }
  });

  it('EXP-36 rejects invented deterministic resolution rates', () => {
    const input = data();

    input.historicalWorkflow.report
      .validationReport!.issueResolutionRate = 100;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors).toContain(
        'Resolution rate must be null when no issues exist.'
      );
    }
  });

  it('EXP-37 rejects invented semantic resolution rates', () => {
    const input = data();

    input.historicalWorkflow.report.validationReport!
      .semanticTraceability!.semanticIssueResolutionRate = 100;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors).toContain(
        'Semantic resolution must remain unconfirmed.'
      );
    }
  });

  it('EXP-38 rejects inconsistent semantic alert counters', () => {
    const input = data();

    input.historicalWorkflow.report.validationReport!
      .semanticTraceability!.evidenceGaps = 3;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Inconsistent semantic alert counters.');
    }
  });

  it('EXP-39 rejects missing semantic traceability', () => {
    const input = data();

    input.historicalWorkflow.report.validationReport!
      .semanticTraceability = null;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Semantic traceability is missing.');
    }
  });

  it('EXP-40 preserves all semantic alerts and official values', () => {
    const input = data();
    const before = structuredClone(input);

    expect(
      validateQualityReportExport(input, context()).status
    ).toBe('VALID');

    expect(input).toEqual(before);

    const report = input.historicalWorkflow.report;

    expect(report.revisedAnalysis!.qis).toBe(68.89);
    expect(report.revisedAnalysis!.releaseDecision)
      .toBe('GO_WITH_RISK');

    expect(report.humanReviewRequired).toBe(true);
    expect(report.semanticQualityCertified).toBe(false);

    expect(
      report.validationReport!.semanticTraceability!.alerts
    ).toHaveLength(5);
  });

  it('EXP-41 combines governance and mathematical errors', () => {
    const input = data();

    input.currentDashboard = dashboard();
    input.currentDashboard.sonar.sonarContribution = 30;

    input.historicalWorkflow.report.validationReport!
      .semanticTraceability!.semanticIssueResolutionRate = 100;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors)
        .toContain('Invalid Sonar contribution.');

      expect(result.errors)
        .toContain('Semantic resolution must remain unconfirmed.');
    }
  });

  it('EXP-42 rejects structurally invalid reports safely', () => {
    const input = data();

    input.historicalWorkflow.report =
      null as unknown as AiQualityReport;

    const result =
      validateQualityReportExport(input, context());

    expect(result.status).toBe('INVALID');

    if (result.status === 'INVALID') {
      expect(result.errors).toContain(
        'Historical AI report is invalid or unavailable.'
      );
    }
  });

});
