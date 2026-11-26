import { AiQualityReport } from './ai-quality-report.model';

export function validateReportGovernance(
  report: AiQualityReport
): string[] {

  const errors: string[] = [];

  // Contract confirmed for workflow V3.5.4 only.
  if (report.run.workflow_version !== 'V3.5.4') {
    return ['Unsupported workflow governance contract.'];
  }

  if (
    report.run.mode !== 'REAL' ||
    report.run.status !== 'COMPLETED'
  ) {
    return ['Governance requires a completed REAL workflow.'];
  }

  const validation = report.validationReport;

  if (!validation) {
    return ['Governance validation report is missing.'];
  }

  if (
    report.humanReviewRequired !== true ||
    validation.humanReviewRequired !== true
  ) {
    errors.push('Human review must remain required.');
  }

  if (
    report.semanticQualityCertified !== false ||
    validation.semanticQualityCertified !== false
  ) {
    errors.push('Semantic quality must not be certified.');
  }

  if (validation.status !== 'PASS_GUARDRAILS') {
    errors.push('Invalid final guardrails status.');
  }

  const validCount = (value: unknown): value is number =>
    typeof value === 'number' &&
    Number.isSafeInteger(value) &&
    value >= 0;

  const detected = validation.detectedIssues;
  const resolved = validation.resolvedIssues;
  const rate = validation.issueResolutionRate;

  if (!validCount(detected) || !validCount(resolved)) {

    errors.push('Invalid deterministic issue counters.');

  } else {

    if (resolved > detected) {
      errors.push('Resolved issues exceed detected issues.');
    }

    if (resolved !== detected) {
      errors.push('Unresolved deterministic issues remain.');
    }

    if (detected === 0) {

      if (rate !== null) {
        errors.push(
          'Resolution rate must be null when no issues exist.'
        );
      }

    } else {

      const expectedRate = 100 * resolved / detected;

      if (
        typeof rate !== 'number' ||
        !Number.isFinite(rate) ||
        Math.abs(rate - expectedRate) > 0.000001
      ) {
        errors.push('Invalid deterministic resolution rate.');
      }

    }
  }

  const semantic = validation.semanticTraceability;

  if (!semantic) {
    errors.push('Semantic traceability is missing.');
    return errors;
  }

  if (semantic.status !== 'HUMAN_REVIEW_REQUIRED') {
    errors.push('Invalid semantic review status.');
  }

  if (
    semantic.confirmedSemanticIssues !== null ||
    semantic.confirmedResolvedSemanticIssues !== null ||
    semantic.semanticIssueResolutionRate !== null
  ) {
    errors.push(
      'Semantic resolution must remain unconfirmed.'
    );
  }

  const alertCount = semantic.alertCount;
  const evaluated = semantic.evaluatedClaims;
  const contradictions = semantic.potentialContradictions;
  const gaps = semantic.evidenceGaps;
  const uniqueFields = semantic.uniqueAlertedFields;
  const changedFields = semantic.changedAlertedFields;

  if (
    !validCount(alertCount) ||
    !validCount(evaluated) ||
    !validCount(contradictions) ||
    !validCount(gaps) ||
    !validCount(uniqueFields) ||
    !validCount(changedFields)
  ) {
    errors.push('Invalid semantic counters.');
    return errors;
  }

  if (alertCount !== contradictions + gaps) {
    errors.push('Inconsistent semantic alert counters.');
  }

  if (evaluated < alertCount) {
    errors.push('Alerts exceed evaluated claims.');
  }

  if (
    changedFields > uniqueFields ||
    uniqueFields > alertCount
  ) {
    errors.push('Inconsistent alerted field counters.');
  }

  const alerts = semantic.alerts;

  if (!Array.isArray(alerts)) {
    errors.push('Semantic alerts are missing.');
    return errors;
  }

  if (alerts.length !== alertCount) {
    errors.push('Semantic alert count mismatch.');
  }

  let actualContradictions = 0;
  let actualGaps = 0;

  const changedPaths = new Set<string>();

  for (const alert of alerts) {

    if (!alert || !alert.assessment) {
      errors.push('Invalid semantic alert assessment.');
      continue;
    }

    const assessment = alert.assessment;

    if (
      alert.status !== 'HUMAN_REVIEW_REQUIRED' ||
      assessment.semanticStatus !== 'HUMAN_REVIEW_REQUIRED'
    ) {
      errors.push('Invalid semantic alert review status.');
    }

    if (assessment.relation === 'CONTRADICTED') {

      actualContradictions++;

      if (
        assessment.findingNature !==
        'POTENTIAL_CONTRADICTION'
      ) {
        errors.push('Invalid contradiction classification.');
      }

    } else if (assessment.relation === 'NOT_ESTABLISHED') {

      actualGaps++;

      if (assessment.findingNature !== 'EVIDENCE_GAP') {
        errors.push('Invalid evidence gap classification.');
      }

    } else {

      errors.push('Unexpected semantic alert relation.');

    }

    if (typeof alert.sourceFieldChanged !== 'boolean') {
      errors.push('Invalid source field change indicator.');
    }

    if (alert.sourceFieldChanged === true) {

      const path = assessment.sourcePath;

      if (typeof path !== 'string' || !path.startsWith('/')) {
        errors.push('Invalid changed source field path.');
      } else {
        changedPaths.add(path);
      }

    }

  }

  if (
    actualContradictions !== contradictions ||
    actualGaps !== gaps
  ) {
    errors.push('Semantic alert classification count mismatch.');
  }

  if (changedPaths.size !== changedFields) {
    errors.push('Changed source field count mismatch.');
  }

  return errors;
}
