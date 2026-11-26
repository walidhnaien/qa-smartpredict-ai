import { describe, it, expect } from 'vitest';
import { validateQualityReport } from './ai-quality-report.validator';
import baseline from './testing/quality-report.fixture.json';

const RUN_ID = baseline.run.id;
const RELEASE_ID = baseline.run.release_id;

const fixture = (): any => structuredClone(baseline);

const validate = (data: unknown) =>
  validateQualityReport(data, RUN_ID, RELEASE_ID);

describe('AI Quality Report Validator', () => {

  it('VAL-01 accepts the real baseline report', () => {
    expect(validate(fixture()).status).toBe('AVAILABLE');
  });

  it('VAL-02 rejects a null response', () => {
    expect(validate(null).status).toBe('ERROR');
  });

  it('VAL-03 rejects an incorrect run ID', () => {
    const data = fixture();
    data.run.id = 'wrong-run';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-04 rejects an incorrect release ID', () => {
    const data = fixture();
    data.run.release_id = 'wrong-release';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-05 rejects simulation reports', () => {
    const data = fixture();
    data.run.mode = 'SIMULATION';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-06 rejects unfinished workflows', () => {
    const data = fixture();
    data.run.status = 'RUNNING';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-07 handles missing analysis', () => {
    const data = fixture();
    delete data.revisedAnalysis;
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-08 handles missing validation', () => {
    const data = fixture();
    delete data.validationReport;
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-09 preserves unavailable metrics', () => {
    const data = fixture();
    data.revisedAnalysis.qis = null;
    data.revisedAnalysis.confidence = null;

    const result = validate(data);

    expect(result.status).toBe('AVAILABLE');

    if (result.status === 'AVAILABLE') {
      expect(result.report.revisedAnalysis?.qis).toBeNull();
      expect(result.report.revisedAnalysis?.confidence).toBeNull();
    }
  });

  it('VAL-10 rejects incorrect metric types', () => {
    const data = fixture();
    data.revisedAnalysis.qis = '68.89';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-11 rejects malformed actions', () => {
    const data = fixture();
    data.revisedAnalysis.priorityActions = {};
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-12 rejects malformed alerts', () => {
    const data = fixture();
    data.validationReport.semanticTraceability.alerts = 'invalid';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-13 rejects inconsistent governance', () => {
    const data = fixture();
    data.semanticQualityCertified = true;
    data.validationReport.semanticQualityCertified = false;
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-14 rejects invalid completion dates', () => {
    const data = fixture();
    data.run.completed_at = 'invalid-date';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-15 preserves official quality metrics', () => {
    const result = validate(fixture());

    expect(result.status).toBe('AVAILABLE');

    if (result.status === 'AVAILABLE') {
      const analysis = result.report.revisedAnalysis;

      expect(analysis?.qis).toBeCloseTo(68.89, 2);
      expect(analysis?.defectScore).toBeCloseTo(22.22, 2);
      expect(analysis?.bugRatio).toBeCloseTo(77.78, 2);
      expect(analysis?.confidence).toBe(95);
      expect(analysis?.riskLevel).toBe('HIGH');
      expect(analysis?.releaseDecision).toBe('GO_WITH_RISK');
      expect(analysis?.priorityActions).toHaveLength(4);
    }
  });

  it('VAL-16 preserves semantic traceability', () => {
    const result = validate(fixture());

    expect(result.status).toBe('AVAILABLE');

    if (result.status === 'AVAILABLE') {
      const report = result.report;
      const validation = report.validationReport;
      const semantic = validation?.semanticTraceability;

      expect(report.humanReviewRequired).toBe(true);
      expect(report.semanticQualityCertified).toBe(false);
      expect(validation?.status).toBe('PASS_GUARDRAILS');

      expect(semantic?.evaluatedClaims).toBe(6);
      expect(semantic?.alertCount).toBe(5);
      expect(semantic?.alerts).toHaveLength(5);
      expect(semantic?.evidenceGaps).toBe(4);
      expect(semantic?.potentialContradictions).toBe(1);
      expect(semantic?.changedAlertedFields).toBe(4);
      expect(semantic?.semanticIssueResolutionRate).toBeNull();
    }
  });

  it('VAL-17 rejects malformed nested assessments', () => {
    const data = fixture();
    data.validationReport.semanticTraceability.alerts[0].assessment = 42;
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-18 rejects invalid governance types', () => {
    const data = fixture();
    data.humanReviewRequired = 'true';
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-19 rejects malformed nested risk fields', () => {
    const data = fixture();
    data.revisedAnalysis.topRisks[0].severity = 42;
    expect(validate(data).status).toBe('ERROR');
  });

  it('VAL-20 does not modify the original report', () => {
    const data = fixture();
    const original = structuredClone(data);

    validate(data);

    expect(data).toEqual(original);
  });


  it('VAL-21 rejects an empty analysis section', () => {
    const data = fixture();
    data.revisedAnalysis = {};
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-22 rejects an empty validation section', () => {
    const data = fixture();
    data.validationReport = {};
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-23 handles a blank risk level', () => {
    const data = fixture();
    data.revisedAnalysis.riskLevel = '   ';
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-24 handles a missing release decision', () => {
    const data = fixture();
    delete data.revisedAnalysis.releaseDecision;
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-25 handles a blank validation status', () => {
    const data = fixture();
    data.validationReport.status = '';
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-26 requires explicit human review status', () => {
    const data = fixture();
    delete data.humanReviewRequired;
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-27 requires explicit semantic certification', () => {
    const data = fixture();
    delete data.semanticQualityCertified;
    expect(validate(data).status).toBe('UNAVAILABLE');
  });

  it('VAL-28 rejects an invalid workflow version type', () => {
    const data = fixture();
    data.run.workflow_version = 354;
    expect(validate(data).status).toBe('ERROR');
  });

});
