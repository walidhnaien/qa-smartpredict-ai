import {
  ExportValidationContext
} from '../../models/quality-report-export.validator';

import {
  createDashboardSnapshot,
  createSimulationSnapshot
} from '../../models/testing/quality-report-export.fixture';

import {
  describe,
  it,
  expect,
  vi,
  afterEach
} from 'vitest';

import { QualityPdfReportService }
  from './quality-pdf-report.service';

import { PdfLayoutEngine }
  from './pdf-layout-engine';

import { QualityReportExportData }
  from '../../models/quality-report-export.model';

import { AiQualityReport }
  from '../../models/ai-quality-report.model';

import { WorkflowStep }
  from '../../models/ai-orchestrator.model';

import fixture
  from '../../models/testing/quality-report.fixture.json';

describe('QualityPdfReportService', () => {

  const service = new QualityPdfReportService();

  function context(): ExportValidationContext {
    return {
      expectedRunId: fixture.run.id,
      expectedReleaseId: fixture.run.release_id,
      stepsSourceRunId: fixture.run.id
    };
  }

  function data(): QualityReportExportData {

    const report = structuredClone(
      fixture
    ) as unknown as AiQualityReport;

    return {
      schemaVersion: '1.0',
      applicationVersion: '4.0.5',
      exportedAt: '2026-01-01T11:00:00Z',

      release: {
        id: report.run.release_id,
        name: 'Synthetic QA Release',
        version: '202601.0.0-TEST',
        type: 'MAJOR',
        status: 'READY_FOR_DECISION'
      },

      historicalWorkflow: {
        report,
        steps: []
      },

      currentDashboard: null,
      simulation: null
    };
  }

  function step(
    order: number,
    name: string
  ): WorkflowStep {

    return {
      step_order: order,
      step_name: name,
      status: 'SUCCESS',
      model: 'test-model',
      attempt: 1,
      duration_ms: 1000,
      error_message: null
    } as WorkflowStep;
  }

  function captureFields(
    input: QualityReportExportData
  ) {

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'field'
    );

    service.generate(input, context());

    return spy.mock.calls;
  }

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('PDF-01 generates a real PDF document', () => {

    const pdf = service.generate(data(), context());

    const bytes = new Uint8Array(
      pdf.output('arraybuffer')
    );

    const signature = new TextDecoder().decode(
      bytes.slice(0, 5)
    );

    expect(signature).toBe('%PDF-');
    expect(bytes.length).toBeGreaterThan(1000);
    expect(pdf.getNumberOfPages()).toBeGreaterThan(1);
  });

  it('PDF-02 rejects a different release', () => {

    const input = data();
    input.release.id = 'different-release';

    expect(() => service.generate(input, context()))
      .toThrow('Selected release mismatch.');
  });

  it('PDF-03 rejects a running workflow', () => {

    const input = data();

    input.historicalWorkflow.report.run.status =
      'RUNNING';

    expect(() => service.generate(input, context()))
      .toThrow();
  });

  it('PDF-04 rejects simulation workflow mode', () => {

    const input = data();

    Object.assign(
      input.historicalWorkflow.report.run,
      { mode: 'SIMULATION' }
    );

    expect(() => service.generate(input, context()))
      .toThrow();
  });

  it('PDF-05 rejects an incomplete report', () => {

    const input = data();

    input.historicalWorkflow.report.revisedAnalysis =
      null;

    expect(() => service.generate(input, context()))
      .toThrow();
  });

  it('PDF-06 preserves official quality metrics', () => {

    const fields = captureFields(data());

    expect(fields).toContainEqual([
      'Quality Index Score', '68.89 / 100'
    ]);

    expect(fields).toContainEqual([
      'Defect Quality Score', '22.22 / 100'
    ]);

    expect(fields).toContainEqual([
      'Bugs per 100 user stories', '77.78'
    ]);

    expect(fields).toContainEqual([
      'Analysis confidence (not a probability of safety)',
      '95.00'
    ]);
  });

  it('PDF-07 preserves the official Java recommendation', () => {

    const fields = captureFields(data());

    expect(fields).toContainEqual([
      'Official risk level', 'HIGH'
    ]);

    expect(fields).toContainEqual([
      'Official Java release recommendation',
      'GO_WITH_RISK'
    ]);
  });

  it('PDF-08 preserves governance flags', () => {

    const fields = captureFields(data());

    expect(fields).toContainEqual([
      'Human review required', 'YES'
    ]);

    expect(fields).toContainEqual([
      'Semantic quality certified', 'NO'
    ]);

    expect(fields).toContainEqual([
      'Guardrail status', 'PASS_GUARDRAILS'
    ]);
  });

  it('PDF-09 renders every priority action', () => {

    const input = data();
    const fields = captureFields(input);

    const actions =
      input.historicalWorkflow.report
        .revisedAnalysis!.priorityActions!;

    expect(actions).toHaveLength(4);

    expect(
      fields.filter(([label]) => label === 'Action')
    ).toHaveLength(4);

    for (const action of actions) {

      expect(fields).toContainEqual([
        'Action', action.action
      ]);

      expect(fields).toContainEqual([
        'Origin', action.origin
      ]);

      expect(fields).toContainEqual([
        'Acceptance criterion',
        action.acceptanceCriterion
      ]);

      expect(fields).toContainEqual([
        'Expected impact', action.expectedImpact
      ]);

      expect(fields).toContainEqual([
        'Evidence',
        typeof action.evidence === 'string'
          ? action.evidence
          : JSON.stringify(action.evidence, null, 2)
      ]);
    }
  });

  it('PDF-10 renders all five semantic alerts', () => {

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'paragraph'
    );

    service.generate(data(), context());

    const alerts = spy.mock.calls.filter(
      ([value]) =>
        typeof value === 'string' &&
        /^Semantic Alert \d+$/.test(value)
    );

    expect(alerts).toHaveLength(5);
  });

  it('PDF-11 preserves all before and after statements', () => {

    const input = data();
    const fields = captureFields(input);

    const alerts =
      input.historicalWorkflow.report
        .validationReport!
        .semanticTraceability!
        .alerts!;

    expect(alerts).toHaveLength(5);

    for (const alert of alerts) {

      expect(fields).toContainEqual([
        'BEFORE - Flagged statement',
        alert.before
      ]);

      expect(fields).toContainEqual([
        'AFTER - Revised statement',
        alert.after
      ]);

      expect(fields).toContainEqual([
        'Evidence',
        alert.assessment?.evidence
      ]);

      expect(fields).toContainEqual([
        'Hypothesis',
        alert.assessment?.hypothesis
      ]);
    }
  });

  it('PDF-12 preserves unestablished resolution values', () => {

    const fields = captureFields(data());

    expect(fields).toContainEqual([
      'Confirmed semantic issues', null
    ]);

    expect(fields).toContainEqual([
      'Confirmed resolved semantic issues', null
    ]);

    expect(fields).toContainEqual([
      'Semantic resolution rate (source value)', null
    ]);
  });

  it('PDF-13 formats missing quality metrics as N/A', () => {

    const input = data();

    input.historicalWorkflow.report
      .revisedAnalysis!.qis = null;

    const fields = captureFields(input);

    expect(fields).toContainEqual([
      'Quality Index Score', 'N/A'
    ]);
  });

  it('PDF-14 preserves zero values', () => {

    const input = data();

    input.historicalWorkflow.report
      .revisedAnalysis!.defectScore = 0;

    const fields = captureFields(input);

    expect(fields).toContainEqual([
      'Defect Quality Score', '0.00 / 100'
    ]);

    expect(fields).toContainEqual([
      'Detected issues', 0
    ]);

    expect(fields).toContainEqual([
      'Resolved issues', 0
    ]);
  });

  it('PDF-15 renders workflow steps in order', () => {

    const input = data();

    input.historicalWorkflow.steps = [
      step(3, 'EXTRACT_CLAIMS'),
      step(1, 'COLLECT_METRICS'),
      step(2, 'INITIAL_ANALYSIS')
    ];

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'paragraph'
    );

    service.generate(input, context());

    const names = spy.mock.calls
      .map(([value]) => value)
      .filter(value =>
        typeof value === 'string' &&
        /^\d+\. [A-Z_]+$/.test(value)
      );

    expect(names).toEqual([
      '1. COLLECT_METRICS',
      '2. INITIAL_ANALYSIS',
      '3. EXTRACT_CLAIMS'
    ]);
  });

  it('PDF-16 reports unavailable workflow steps', () => {

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'paragraph'
    );

    service.generate(data(), context());

    expect(spy).toHaveBeenCalledWith(
      'Workflow steps unavailable.'
    );
  });

  it('PDF-17 paginates very long semantic statements', () => {

    const input = data();

    const baselinePages =
      service.generate(input, context()).getNumberOfPages();

    const alert =
      input.historicalWorkflow.report
        .validationReport!
        .semanticTraceability!
        .alerts![0];

    alert.before =
      'Long semantic evidence statement. '.repeat(2000);

    const pdf = service.generate(input, context());

    expect(pdf.getNumberOfPages())
      .toBeGreaterThan(baselinePages);
  });

  it('PDF-18 does not mutate export data', () => {

    const input = data();

    input.historicalWorkflow.steps = [
      step(2, 'INITIAL_ANALYSIS'),
      step(1, 'COLLECT_METRICS')
    ];

    const before = structuredClone(input);

    service.generate(input, context());

    expect(input).toEqual(before);
  });

  it('PDF-19 explicitly identifies historical scope', () => {

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'paragraph'
    );

    service.generate(data(), context());

    expect(spy).toHaveBeenCalledWith(
      expect.stringContaining(
        'Scope: historical AI workflow assessment'
      )
    );
  });

  it('PDF-20 preserves human release responsibility', () => {

    const spy = vi.spyOn(
      PdfLayoutEngine.prototype,
      'paragraph'
    );

    service.generate(data(), context());

    expect(spy).toHaveBeenCalledWith(
      'This report does not constitute release approval.',
      { bold: true }
    );

    expect(spy).toHaveBeenCalledWith(
      expect.stringContaining(
        'authorized human stakeholders'
      ),
      { bold: true }
    );
  });

  function completeData(): QualityReportExportData {
    const input = data();

    input.currentDashboard =
      createDashboardSnapshot(input.release.id);

    input.simulation =
      createSimulationSnapshot(input.release.id);

    return input;
  }

  it('PDF-21 rejects a mismatched Run ID', () => {
    const input = data();
    input.historicalWorkflow.report.run.id = 'wrong-run';

    expect(() => service.generate(input, context()))
      .toThrow('Historical AI report is invalid or unavailable.');
  });

  it('PDF-22 rejects incorrect workflow step provenance', () => {
    const input = data();

    input.historicalWorkflow.steps = [
      step(1, 'COLLECT_METRICS')
    ];

    const ctx = context();
    ctx.stepsSourceRunId = 'wrong-run';

    expect(() => service.generate(input, ctx))
      .toThrow('Workflow steps source mismatch.');
  });

  it('PDF-23 includes complete release identification', () => {
    const fields = captureFields(data());

    expect(fields).toContainEqual([
      'Release name', 'Synthetic QA Release'
    ]);

    expect(fields).toContainEqual([
      'Release version', '202601.0.0-TEST'
    ]);

    expect(fields).toContainEqual([
      'Release type', 'MAJOR'
    ]);

    expect(fields).toContainEqual([
      'Release status at export', 'READY_FOR_DECISION'
    ]);
  });

  it('PDF-24 separates historical and current QIS', () => {
    const fields = captureFields(completeData());

    expect(fields).toContainEqual([
      'Quality Index Score', '68.89 / 100'
    ]);

    expect(fields).toContainEqual([
      'Current official QIS', '72.00 / 100'
    ]);
  });

  it('PDF-25 blocks invalid dashboard mathematics', () => {
    const input = completeData();
    input.currentDashboard!.sonar.sonarContribution = 99;

    expect(() => service.generate(input, context()))
      .toThrow('Invalid Sonar contribution.');
  });

  it('PDF-26 blocks invalid simulation mathematics', () => {
    const input = completeData();
    input.simulation!.simulatedQis = 95;

    expect(() => service.generate(input, context()))
      .toThrow('Invalid simulated QIS calculation.');
  });

  it('PDF-27 rejects simulation without dashboard', () => {
    const input = completeData();
    input.currentDashboard = null;

    expect(() => service.generate(input, context()))
      .toThrow('Simulation requires a dashboard snapshot.');
  });

  it('PDF-28 explicitly reports unavailable dashboard', () => {
    const spy = vi.spyOn(
      PdfLayoutEngine.prototype, 'paragraph'
    );

    service.generate(data(), context());

    expect(spy).toHaveBeenCalledWith(
      'Current dashboard unavailable.'
    );
  });

  it('PDF-29 explicitly reports unavailable simulation', () => {
    const spy = vi.spyOn(
      PdfLayoutEngine.prototype, 'paragraph'
    );

    service.generate(data(), context());

    expect(spy).toHaveBeenCalledWith(
      'What-if simulation unavailable.'
    );
  });

  it('PDF-30 preserves missing Sonar and Java fallback', () => {
    const input = completeData();
    input.simulation = null;

    const d = input.currentDashboard!;

    Object.assign(d.sonar, {
      sonarScore: null,
      sonarWeight: 0,
      sonarContribution: 0,
      baseQisContribution: 70,
      finalQis: 70,
      sonarQualityGate: 'NOT_AVAILABLE'
    });

    d.finalQis = 70;

    const fields = captureFields(input);

    expect(fields).toContainEqual([
      'Sonar Quality Score', 'N/A'
    ]);

    expect(fields).toContainEqual([
      'Current official QIS', '70.00 / 100'
    ]);

    expect(fields).toContainEqual([
      'Official Sonar weight', '0.00%'
    ]);
  });

  it('PDF-31 preserves simulation metrics and decisions', () => {
    const fields = captureFields(completeData());

    expect(fields).toContainEqual([
      'Simulation baseline QIS', '72.00'
    ]);

    expect(fields).toContainEqual([
      'Simulated QIS', '80.00'
    ]);

    expect(fields).toContainEqual([
      'Simulated QIS difference', '8.00 points'
    ]);

    expect(fields).toContainEqual([
      'Baseline threshold decision', 'GO WITH RISK'
    ]);

    expect(fields).toContainEqual([
      'Simulated threshold decision', 'GO'
    ]);

    expect(fields).toContainEqual([
      'Official Java release recommendation', 'GO_WITH_RISK'
    ]);
  });

  it('PDF-32 renders all six simulation weights', () => {
    const fields = captureFields(completeData());

    const weights = fields
      .filter(([label]) => label === 'Simulation weight')
      .map(([, value]) => value);

    expect(weights).toEqual([
      '16.00%', '16.00%', '16.00%',
      '16.00%', '16.00%', '20.00%'
    ]);
  });

  it('PDF-33 preserves the What-if recommendation', () => {
    const fields = captureFields(completeData());

    expect(fields).toContainEqual([
      'Recommended metric', 'Defect Quality'
    ]);

    expect(fields).toContainEqual([
      'Recommended target value', '80.00'
    ]);

    expect(fields).toContainEqual([
      'Required improvement', '50.00 points'
    ]);

    expect(fields).toContainEqual([
      'Recommended projected QIS', '80.00'
    ]);
  });

  it('PDF-34 preserves warnings and simulation limitations', () => {
    const spy = vi.spyOn(
      PdfLayoutEngine.prototype, 'paragraph'
    );

    service.generate(completeData(), context());

    expect(spy).toHaveBeenCalledWith(
      'Workflow steps unavailable.'
    );

    expect(spy).toHaveBeenCalledWith(
      expect.stringContaining('HYPOTHETICAL SIMULATION ONLY.'),
      { bold: true }
    );

    expect(spy).toHaveBeenCalledWith(
      expect.stringContaining(
        'It does not certify semantic correctness'
      ),
      { bold: true }
    );
  });

  it('PDF-35 does not mutate complete export data or context', () => {
    const input = completeData();
    const ctx = context();

    const before = structuredClone(input);
    const contextBefore = structuredClone(ctx);

    service.generate(input, ctx);

    expect(input).toEqual(before);
    expect(ctx).toEqual(contextBefore);
  });

});
