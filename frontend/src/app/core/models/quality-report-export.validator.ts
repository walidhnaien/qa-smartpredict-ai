import {
  validateReportGovernance
} from './quality-report-governance.validator';

import {
  validateDashboardMath,
  validateSimulationMath
} from './quality-report-math.validator';

import {
  QualityReportExportData
} from './quality-report-export.model';

import {
  validateQualityReport
} from './ai-quality-report.validator';

export interface ExportValidationContext {
  expectedRunId: string;
  expectedReleaseId: string;
  stepsSourceRunId: string | null;
}

export type ExportValidationResult =
  | {
      status: 'VALID';
      warnings: string[];
    }
  | {
      status: 'INVALID';
      errors: string[];
      warnings: string[];
    };

export function validateQualityReportExport(
  data: QualityReportExportData,
  context: ExportValidationContext
): ExportValidationResult {

  const errors: string[] = [];
  const warnings: string[] = [];

  const validDate = (value: unknown): boolean =>
    typeof value === 'string' &&
    Number.isFinite(Date.parse(value));

  const validId = (value: unknown): boolean =>
    typeof value === 'string' &&
    value.trim().length > 0;

  if (!validId(context.expectedRunId)) {
    errors.push('Expected Run ID is missing.');
  }

  if (!validId(context.expectedReleaseId)) {
    errors.push('Expected Release ID is missing.');
  }

  if (data.schemaVersion !== '1.0') {
    errors.push('Unsupported export schema.');
  }

  if (data.applicationVersion !== '4.0.5') {
    errors.push('Unexpected application version.');
  }

  if (!validDate(data.exportedAt)) {
    errors.push('Invalid export date.');
  }

  if (data.release.id !== context.expectedReleaseId) {
    errors.push('Selected release mismatch.');
  }

  const reportValidation = validateQualityReport(
    data.historicalWorkflow.report,
    context.expectedRunId,
    context.expectedReleaseId
  );

  if (reportValidation.status !== 'AVAILABLE') {
    errors.push('Historical AI report is invalid or unavailable.');
  } else {
    // Governance contract for the historical AI workflow.
    errors.push(
      ...validateReportGovernance(reportValidation.report)
    );
  }

  const steps = data.historicalWorkflow.steps;

  if (!Array.isArray(steps)) {

    errors.push('Invalid workflow steps.');

  } else if (steps.length === 0) {

    warnings.push('Workflow steps unavailable.');

  } else {

    if (
      context.stepsSourceRunId !==
      context.expectedRunId
    ) {
      errors.push('Workflow steps source mismatch.');
    }

    const orders = new Set<number>();

    for (const step of steps) {

      if (
        !step ||
        !Number.isInteger(step.step_order) ||
        step.step_order < 1
      ) {
        errors.push('Invalid workflow step order.');
        continue;
      }

      if (orders.has(step.step_order)) {
        errors.push('Duplicate workflow step order.');
      }

      orders.add(step.step_order);

      if (!validId(step.step_name)) {
        errors.push('Invalid workflow step name.');
      }
    }
  }

  const dashboard = data.currentDashboard;

  if (dashboard == null) {

    warnings.push('Current dashboard unavailable.');

  } else {

    if (
      dashboard.releaseId !==
      context.expectedReleaseId
    ) {
      errors.push('Dashboard release mismatch.');
    }

    if (!validDate(dashboard.capturedAt)) {
      errors.push('Invalid dashboard capture date.');
    }

    if (!Number.isFinite(dashboard.finalQis)) {
      errors.push('Invalid dashboard QIS.');
    }
  }

  const simulation = data.simulation;

  if (simulation == null) {

    warnings.push('What-if simulation unavailable.');

  } else {

    if (
      simulation.releaseId !==
      context.expectedReleaseId
    ) {
      errors.push('Simulation release mismatch.');
    }

    if (!validDate(simulation.capturedAt)) {
      errors.push('Invalid simulation capture date.');
    }

    const values = [
      simulation.currentQis,
      simulation.simulatedQis,
      simulation.difference,
      simulation.targetQis,
      simulation.gapToGo
    ];

    if (!values.every(Number.isFinite)) {
      errors.push('Invalid simulation metrics.');
    }
  }

  // Mathematical consistency checks.
  // Historical workflow metrics remain independent
  // from the current dashboard and simulation.

  if (dashboard != null) {
    errors.push(...validateDashboardMath(dashboard));
  }

  if (simulation != null) {
    errors.push(
      ...validateSimulationMath(
        simulation,
        dashboard
      )
    );
  }

  if (errors.length > 0) {
    return {
      status: 'INVALID',
      errors,
      warnings
    };
  }

  return {
    status: 'VALID',
    warnings
  };
}
