import { describe, expect, it } from 'vitest';

import { AiQualityReport } from './ai-quality-report.model';

import {
  validateReportGovernance
} from './quality-report-governance.validator';

import fixture from './testing/quality-report.fixture.json';

function createReport(): AiQualityReport {
  return structuredClone(fixture) as unknown as AiQualityReport;
}

describe('Quality report governance validator', () => {

  // A. GENERAL GOVERNANCE

  it('GOV-01 accepts the nominal report', () => {
    expect(validateReportGovernance(createReport())).toEqual([]);
  });

  it('GOV-02 rejects an unsupported workflow version', () => {
    const r = createReport();
    r.run.workflow_version = 'V9.0.0';

    expect(validateReportGovernance(r))
      .toContain('Unsupported workflow governance contract.');
  });

  it('GOV-03 rejects a non-REAL workflow', () => {
    const r = createReport();
    r.run.mode = 'SIMULATION' as typeof r.run.mode;

    expect(validateReportGovernance(r))
      .toContain('Governance requires a completed REAL workflow.');
  });

  it('GOV-04 rejects a running workflow', () => {
    const r = createReport();
    r.run.status = 'RUNNING';

    expect(validateReportGovernance(r))
      .toContain('Governance requires a completed REAL workflow.');
  });

  it('GOV-05 rejects disabled root human review', () => {
    const r = createReport();
    r.humanReviewRequired = false;

    expect(validateReportGovernance(r))
      .toContain('Human review must remain required.');
  });

  it('GOV-06 rejects disabled validation human review', () => {
    const r = createReport();
    r.validationReport!.humanReviewRequired = false;

    expect(validateReportGovernance(r))
      .toContain('Human review must remain required.');
  });

  it('GOV-07 rejects root semantic certification', () => {
    const r = createReport();
    r.semanticQualityCertified = true;

    expect(validateReportGovernance(r))
      .toContain('Semantic quality must not be certified.');
  });

  it('GOV-08 rejects validation semantic certification', () => {
    const r = createReport();
    r.validationReport!.semanticQualityCertified = true;

    expect(validateReportGovernance(r))
      .toContain('Semantic quality must not be certified.');
  });

  it('GOV-09 rejects missing validation report', () => {
    const r = createReport();
    r.validationReport = null;

    expect(validateReportGovernance(r))
      .toContain('Governance validation report is missing.');
  });

  it('GOV-10 rejects invalid final guardrails', () => {
    const r = createReport();
    r.validationReport!.status = 'REQUIRES_REVIEW';

    expect(validateReportGovernance(r))
      .toContain('Invalid final guardrails status.');
  });

  // B. DETERMINISTIC RESOLUTION

  it('GOV-11 rejects negative issue counters', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = -1;

    expect(validateReportGovernance(r))
      .toContain('Invalid deterministic issue counters.');
  });

  it('GOV-12 rejects fractional issue counters', () => {
    const r = createReport();
    r.validationReport!.resolvedIssues = 0.5;

    expect(validateReportGovernance(r))
      .toContain('Invalid deterministic issue counters.');
  });

  it('GOV-13 rejects resolved issues exceeding detected issues', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 1;
    r.validationReport!.resolvedIssues = 2;

    expect(validateReportGovernance(r))
      .toContain('Resolved issues exceed detected issues.');
  });

  it('GOV-14 rejects unresolved deterministic issues', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 2;
    r.validationReport!.resolvedIssues = 1;
    r.validationReport!.issueResolutionRate = 50;

    expect(validateReportGovernance(r))
      .toContain('Unresolved deterministic issues remain.');
  });

  it('GOV-15 accepts zero issues with null resolution rate', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 0;
    r.validationReport!.resolvedIssues = 0;
    r.validationReport!.issueResolutionRate = null;

    expect(validateReportGovernance(r)).toEqual([]);
  });

  it('GOV-16 rejects 100 percent resolution with zero issues', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 0;
    r.validationReport!.resolvedIssues = 0;
    r.validationReport!.issueResolutionRate = 100;

    expect(validateReportGovernance(r))
      .toContain(
        'Resolution rate must be null when no issues exist.'
      );
  });

  it('GOV-17 accepts fully resolved deterministic issues', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 3;
    r.validationReport!.resolvedIssues = 3;
    r.validationReport!.issueResolutionRate = 100;

    expect(validateReportGovernance(r)).toEqual([]);
  });

  it('GOV-18 rejects incorrect deterministic resolution rate', () => {
    const r = createReport();
    r.validationReport!.detectedIssues = 3;
    r.validationReport!.resolvedIssues = 3;
    r.validationReport!.issueResolutionRate = 90;

    expect(validateReportGovernance(r))
      .toContain('Invalid deterministic resolution rate.');
  });

  // C. SEMANTIC TRACEABILITY

  it('GOV-19 rejects missing semantic traceability', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability = null;

    expect(validateReportGovernance(r))
      .toContain('Semantic traceability is missing.');
  });

  it('GOV-20 rejects an invalid semantic review status', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.status = 'PASS';

    expect(validateReportGovernance(r))
      .toContain('Invalid semantic review status.');
  });

  it('GOV-21 rejects confirmed semantic issues', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!
      .confirmedSemanticIssues = 5;

    expect(validateReportGovernance(r))
      .toContain('Semantic resolution must remain unconfirmed.');
  });

  it('GOV-22 rejects confirmed resolved semantic issues', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!
      .confirmedResolvedSemanticIssues = 5;

    expect(validateReportGovernance(r))
      .toContain('Semantic resolution must remain unconfirmed.');
  });

  it('GOV-23 rejects invented semantic resolution rate', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!
      .semanticIssueResolutionRate = 100;

    expect(validateReportGovernance(r))
      .toContain('Semantic resolution must remain unconfirmed.');
  });

  it('GOV-24 rejects negative semantic counters', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.alertCount = -1;

    expect(validateReportGovernance(r))
      .toContain('Invalid semantic counters.');
  });

  it('GOV-25 rejects fractional semantic counters', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.evaluatedClaims = 6.5;

    expect(validateReportGovernance(r))
      .toContain('Invalid semantic counters.');
  });

  it('GOV-26 rejects missing semantic alerts', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.alerts = null;

    expect(validateReportGovernance(r))
      .toContain('Semantic alerts are missing.');
  });

  it('GOV-27 rejects missing alert details', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.alerts = [];

    expect(validateReportGovernance(r))
      .toContain('Semantic alert count mismatch.');
  });

  it('GOV-28 rejects inconsistent alert totals', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.evidenceGaps = 3;

    expect(validateReportGovernance(r))
      .toContain('Inconsistent semantic alert counters.');
  });

  it('GOV-29 rejects alerts exceeding evaluated claims', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.evaluatedClaims = 4;

    expect(validateReportGovernance(r))
      .toContain('Alerts exceed evaluated claims.');
  });

  it('GOV-30 rejects changed fields exceeding unique fields', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.changedAlertedFields = 5;

    expect(validateReportGovernance(r))
      .toContain('Inconsistent alerted field counters.');
  });

  it('GOV-31 rejects incorrect changed field count', () => {
    const r = createReport();
    r.validationReport!.semanticTraceability!.changedAlertedFields = 3;

    expect(validateReportGovernance(r))
      .toContain('Changed source field count mismatch.');
  });

  it('GOV-32 rejects an incorrect contradiction classification', () => {
    const r = createReport();
    const alert = r.validationReport!.semanticTraceability!.alerts!
      .find(a => a.assessment?.relation === 'CONTRADICTED')!;

    alert.assessment!.findingNature = 'EVIDENCE_GAP';

    expect(validateReportGovernance(r))
      .toContain('Invalid contradiction classification.');
  });

  it('GOV-33 rejects an incorrect evidence gap classification', () => {
    const r = createReport();
    const alert = r.validationReport!.semanticTraceability!.alerts!
      .find(a => a.assessment?.relation === 'NOT_ESTABLISHED')!;

    alert.assessment!.findingNature = 'POTENTIAL_CONTRADICTION';

    expect(validateReportGovernance(r))
      .toContain('Invalid evidence gap classification.');
  });

  it('GOV-34 rejects SUPPORTED claims inside alerts', () => {
    const r = createReport();
    const alert = r.validationReport!.semanticTraceability!.alerts![0];

    alert.assessment!.relation = 'SUPPORTED';

    expect(validateReportGovernance(r))
      .toContain('Unexpected semantic alert relation.');
  });

  it('GOV-35 accepts multiple alerts on the same source field', () => {
    const r = createReport();
    const s = r.validationReport!.semanticTraceability!;

    expect(s.alertCount).toBe(5);
    expect(s.uniqueAlertedFields).toBe(4);
    expect(s.changedAlertedFields).toBe(4);

    expect(validateReportGovernance(r)).toEqual([]);
  });

  it('GOV-36 accepts zero semantic alerts without certification', () => {
    const r = createReport();
    const s = r.validationReport!.semanticTraceability!;

    s.alertCount = 0;
    s.evaluatedClaims = 0;
    s.potentialContradictions = 0;
    s.evidenceGaps = 0;
    s.uniqueAlertedFields = 0;
    s.changedAlertedFields = 0;
    s.alerts = [];

    expect(validateReportGovernance(r)).toEqual([]);
    expect(r.semanticQualityCertified).toBe(false);
    expect(r.humanReviewRequired).toBe(true);
  });

  // D. DATA INTEGRITY

  it('GOV-37 does not modify the source report', () => {
    const r = createReport();
    const before = structuredClone(r);

    validateReportGovernance(r);

    expect(r).toEqual(before);
  });

  it('GOV-38 preserves the official Java decision and QIS', () => {
    const r = createReport();

    const decision = r.revisedAnalysis!.releaseDecision;
    const qis = r.revisedAnalysis!.qis;

    expect(validateReportGovernance(r)).toEqual([]);

    expect(r.revisedAnalysis!.releaseDecision).toBe(decision);
    expect(r.revisedAnalysis!.qis).toBe(qis);
  });

});
