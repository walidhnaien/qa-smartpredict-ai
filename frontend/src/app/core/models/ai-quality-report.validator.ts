import { AiQualityReport } from './ai-quality-report.model';

export type ReportValidation =
  | { status: 'AVAILABLE'; report: AiQualityReport }
  | { status: 'UNAVAILABLE' }
  | { status: 'ERROR'; message: string };

type JsonObject = Record<string, unknown>;

function isObject(value: unknown): value is JsonObject {
  return typeof value === 'object'
    && value !== null
    && !Array.isArray(value);
}

function invalid(message: string): ReportValidation {
  return { status: 'ERROR', message };
}

function validNumbers(
  object: JsonObject,
  fields: string[]
): boolean {
  return fields.every(field => {
    const value = object[field];
    return value == null ||
      (typeof value === 'number' && Number.isFinite(value));
  });
}

function validStrings(
  object: JsonObject,
  fields: string[]
): boolean {
  return fields.every(field =>
    object[field] == null ||
    typeof object[field] === 'string'
  );
}

function validBooleans(
  object: JsonObject,
  fields: string[]
): boolean {
  return fields.every(field =>
    object[field] == null ||
    typeof object[field] === 'boolean'
  );
}

function validObjects(
  value: unknown,
  check: (item: JsonObject) => boolean
): boolean {
  return value == null ||
    (Array.isArray(value) &&
      value.every(item => isObject(item) && check(item)));
}

export function validateQualityReport(
  value: unknown,
  runId: string,
  releaseId: string
): ReportValidation {

  if (!isObject(value) || !isObject(value['run'])) {
    return invalid('Invalid report structure.');
  }

  const run = value['run'];

  if (
    run['id'] !== runId ||
    run['release_id'] !== releaseId ||
    run['mode'] !== 'REAL' ||
    run['status'] !== 'COMPLETED'
  ) {
    return invalid('Report identity or workflow status mismatch.');
  }

  if (!validNumbers(run, ['duration_ms', 'recovery_count'])) {
    return invalid('Invalid execution metrics.');
  }

  if (!validStrings(run, ['workflow_version'])) {
    return invalid('Invalid workflow version.');
  }

  const completedAt = run['completed_at'];

  if (completedAt != null &&
      (typeof completedAt !== 'string' ||
       !Number.isFinite(Date.parse(completedAt)))) {
    return invalid('Invalid completion date.');
  }

  const analysis = value['revisedAnalysis'];
  const validation = value['validationReport'];

  if (analysis == null || validation == null) {
    return { status: 'UNAVAILABLE' };
  }

  if (!isObject(analysis) || !isObject(validation)) {
    return invalid('Invalid report sections.');
  }

  if (!validNumbers(analysis, [
    'qis', 'defectScore', 'bugRatio', 'confidence'
  ])) {
    return invalid('Invalid quality metric types.');
  }

  if (!validStrings(analysis, [
    'mainRisk', 'riskLevel', 'releaseDecision',
    'executiveSummary', 'releaseRationale',
    'confidenceExplanation', 'managerMessage'
  ])) {
    return invalid('Invalid analysis text fields.');
  }

  if (!validObjects(analysis['topRisks'], risk =>
    validStrings(risk, ['dimension', 'severity', 'reason'])
  )) {
    return invalid('Invalid risk details.');
  }

  if (!validObjects(analysis['priorityActions'], action =>
    validNumbers(action, ['priority']) &&
    validStrings(action, [
      'action', 'origin', 'expectedImpact',
      'acceptanceCriterion'
    ])
  )) {
    return invalid('Invalid priority actions.');
  }

  const governanceFields = [
    'humanReviewRequired', 'semanticQualityCertified'
  ];

  if (!validBooleans(value, governanceFields) ||
      !validBooleans(validation, governanceFields)) {
    return invalid('Invalid governance fields.');
  }

  for (const field of governanceFields) {
    if (value[field] != null &&
        validation[field] != null &&
        value[field] !== validation[field]) {
      return invalid('Inconsistent governance fields.');
    }
  }

  if (!validNumbers(validation, [
    'detectedIssues', 'resolvedIssues', 'issueResolutionRate'
  ]) || !validStrings(validation, ['status', 'note'])) {
    return invalid('Invalid validation metrics.');
  }

  const semantic = validation['semanticTraceability'];

  if (semantic != null) {

    if (!isObject(semantic)) {
      return invalid('Invalid semantic traceability.');
    }

    if (!validNumbers(semantic, [
      'alertCount',
      'evidenceGaps',
      'evaluatedClaims',
      'uniqueAlertedFields',
      'changedAlertedFields',
      'potentialContradictions',
      'confirmedSemanticIssues',
      'confirmedResolvedSemanticIssues',
      'semanticIssueResolutionRate'
    ]) || !validStrings(semantic, ['status'])) {
      return invalid('Invalid semantic metrics.');
    }

    if (!validObjects(semantic['alerts'], alert => {

      if (!validStrings(alert, [
        'before', 'after', 'model', 'status'
      ]) || !validBooleans(alert, ['sourceFieldChanged'])) {
        return false;
      }

      const assessment = alert['assessment'];

      return assessment == null ||
        (isObject(assessment) &&
          validStrings(assessment, [
            'premise', 'evidence', 'relation',
            'hypothesis', 'sourcePath', 'referenceId',
            'statementId', 'findingNature'
          ]));
    })) {
      return invalid('Invalid semantic alerts.');
    }
  }

  const requiredText = (field: unknown): boolean =>
    typeof field === 'string' && field.trim().length > 0;

  if (
    !requiredText(analysis['riskLevel']) ||
    !requiredText(analysis['releaseDecision']) ||
    !requiredText(validation['status']) ||
    typeof value['humanReviewRequired'] !== 'boolean' ||
    typeof value['semanticQualityCertified'] !== 'boolean'
  ) {
    return { status: 'UNAVAILABLE' };
  }

  return {
    status: 'AVAILABLE',
    report: value as unknown as AiQualityReport
  };
}
