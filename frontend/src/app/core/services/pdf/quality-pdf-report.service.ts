import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';

import {
  QualityReportExportData
} from '../../models/quality-report-export.model';

import {
  validateQualityReportExport,
  ExportValidationContext
} from '../../models/quality-report-export.validator';

import { PdfLayoutEngine } from './pdf-layout-engine';

@Injectable({
  providedIn: 'root'
})
export class QualityPdfReportService {

  generate(
    data: QualityReportExportData,
    context: ExportValidationContext
  ): jsPDF {

    const validation =
      validateQualityReportExport(data, context);

    if (validation.status === 'INVALID') {
      throw new Error(
        validation.errors.join(' | ')
      );
    }

    const pdf = new PdfLayoutEngine();

    pdf.document.setProperties({
      title: 'QA SmartPredict - Quality Assessment Report',
      subject: 'Quality assessment and AI governance',
      creator: 'QA SmartPredict AI V4.0.5'
    });

    this.renderExecutiveSummary(pdf, data);
    this.renderQualityMetrics(pdf, data);
    this.renderWorkflow(pdf, data);
    this.renderRisks(pdf, data);
    this.renderPriorityActions(pdf, data);
    this.renderGovernance(pdf, data);
    this.renderSemanticTraceability(pdf, data);
    this.renderCurrentDashboard(pdf, data);
    this.renderWhatIfSimulation(pdf, data);
    this.renderExportWarnings(pdf, validation.warnings);
    this.renderConclusion(pdf, data);

    return pdf.finalize();
  }

  private formatNumber(
    value: number | null | undefined,
    suffix = ''
  ): string {

    return typeof value === 'number' &&
      Number.isFinite(value)
      ? value.toFixed(2) + suffix
      : 'N/A';
  }

  private formatBoolean(
    value: boolean | null | undefined
  ): string {

    return value === true
      ? 'YES'
      : value === false
        ? 'NO'
        : 'N/A';
  }

  private formatEvidence(value: unknown): string {

    if (value == null) {
      return 'N/A';
    }

    if (typeof value === 'string') {
      return value;
    }

    return JSON.stringify(value, null, 2) ?? 'N/A';
  }

  private renderExecutiveSummary(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const report = data.historicalWorkflow.report;
    const analysis = report.revisedAnalysis;

    pdf.section('Quality Assessment Report');

    pdf.field('Application version',
      data.applicationVersion);

    pdf.field('Report schema', data.schemaVersion);
    pdf.field('Exported at', data.exportedAt);

    pdf.field('Release ID', report.run.release_id);
    pdf.field('Run ID', report.run.id);

    pdf.field('Release name', data.release.name);
    pdf.field('Release version', data.release.version);
    pdf.field('Release type', data.release.type);
    pdf.field('Release status at export', data.release.status);


    pdf.paragraph(
      'Source: historical completed AI workflow.',
      { bold: true }
    );

    pdf.section('1. Executive Summary');

    pdf.field(
      'Quality Index Score',
      this.formatNumber(analysis?.qis, ' / 100')
    );

    pdf.field(
      'Official risk level',
      analysis?.riskLevel
    );

    pdf.field(
      'Official Java release recommendation',
      analysis?.releaseDecision
    );

    pdf.field(
      'Executive analysis',
      analysis?.executiveSummary
    );

    pdf.field(
      'Release rationale',
      analysis?.releaseRationale
    );

    pdf.field(
      'Management message',
      analysis?.managerMessage
    );

    pdf.paragraph(
      'This report does not constitute release approval.',
      { bold: true }
    );
  }

  private renderQualityMetrics(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const analysis =
      data.historicalWorkflow.report.revisedAnalysis;

    pdf.section('2. Historical Official Quality Metrics');

    pdf.field(
      'Quality Index Score',
      this.formatNumber(analysis?.qis, ' / 100')
    );

    pdf.field(
      'Defect Quality Score',
      this.formatNumber(analysis?.defectScore, ' / 100')
    );

    pdf.field(
      'Bugs per 100 user stories',
      this.formatNumber(analysis?.bugRatio)
    );

    pdf.field(
      'Main risk dimension',
      analysis?.mainRisk
    );

    pdf.field(
      'Analysis confidence (not a probability of safety)',
      this.formatNumber(analysis?.confidence)
    );

    pdf.field(
      'Confidence explanation',
      analysis?.confidenceExplanation
    );
  }

  private renderWorkflow(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const { report, steps } = data.historicalWorkflow;
    const run = report.run;

    pdf.section('3. AI Workflow Execution');

    pdf.field('Workflow version', run.workflow_version);
    pdf.field('Mode', run.mode);
    pdf.field('Status', run.status);
    pdf.field('Started at', run.started_at);
    pdf.field('Completed at', run.completed_at);

    pdf.field(
      'Duration',
      this.formatNumber(
        run.duration_ms == null
          ? null
          : run.duration_ms / 1000,
        ' seconds'
      )
    );

    pdf.field('Recoveries', run.recovery_count);

    pdf.field('Validation ID', run.validation_id);
    pdf.field('Batch ID', run.batch_id);
    pdf.field('Revision ID', run.revision_id);

    if (!steps.length) {
      pdf.paragraph('Workflow steps unavailable.');
      return;
    }

    const ordered = [...steps].sort(
      (a, b) => a.step_order - b.step_order
    );

    for (const step of ordered) {

      pdf.ensureSpace(35);

      pdf.paragraph(
        `${step.step_order}. ${step.step_name}`,
        { bold: true, fontSize: 12 }
      );

      pdf.field('Status', step.status);
      pdf.field('Model', step.model);
      pdf.field('Attempt', step.attempt);

      pdf.field(
        'Duration',
        this.formatNumber(
          step.duration_ms == null
            ? null
            : step.duration_ms / 1000,
          ' seconds'
        )
      );

      if (step.error_message) {
        pdf.field('Error', step.error_message);
      }
    }
  }

  private renderRisks(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const risks =
      data.historicalWorkflow.report
        .revisedAnalysis?.topRisks;

    pdf.section('4. Risk Assessment');

    if (!risks?.length) {
      pdf.paragraph('No risk details available.');
      return;
    }

    for (const [index, risk] of risks.entries()) {

      pdf.ensureSpace(30);

      pdf.paragraph(
        `Risk ${index + 1}`,
        { bold: true, fontSize: 12 }
      );

      pdf.field('Dimension', risk.dimension);
      pdf.field('Severity', risk.severity);
      pdf.field('Reason', risk.reason);
    }
  }

  private renderPriorityActions(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const actions =
      data.historicalWorkflow.report
        .revisedAnalysis?.priorityActions;

    pdf.section('5. Priority Actions');

    if (!actions?.length) {
      pdf.paragraph('No priority actions available.');
      return;
    }

    for (const [index, action] of actions.entries()) {

      pdf.ensureSpace(35);

      pdf.paragraph(
        `Action ${index + 1}`,
        { bold: true, fontSize: 12 }
      );

      pdf.field('Priority', action.priority);
      pdf.field('Action', action.action);
      pdf.field('Origin', action.origin);

      pdf.field(
        'Evidence',
        this.formatEvidence(action.evidence)
      );

      pdf.field(
        'Expected impact',
        action.expectedImpact
      );

      pdf.field(
        'Acceptance criterion',
        action.acceptanceCriterion
      );
    }
  }

  private renderGovernance(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const report = data.historicalWorkflow.report;
    const validation = report.validationReport;

    pdf.section('6. AI Governance and Validation');

    pdf.field('Guardrail status', validation?.status);

    pdf.field(
      'Detected issues',
      validation?.detectedIssues
    );

    pdf.field(
      'Resolved issues',
      validation?.resolvedIssues
    );

    pdf.field(
      'Issue resolution rate (source value)',
      validation?.issueResolutionRate
    );

    pdf.field(
      'Human review required',
      this.formatBoolean(report.humanReviewRequired)
    );

    pdf.field(
      'Semantic quality certified',
      this.formatBoolean(report.semanticQualityCertified)
    );

    pdf.field('Validation note', validation?.note);

    pdf.paragraph(
      'Passing deterministic guardrails does not establish ' +
      'factual completeness or semantic correctness.',
      { bold: true }
    );
  }

  private renderSemanticTraceability(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const semantic =
      data.historicalWorkflow.report
        .validationReport?.semanticTraceability;

    pdf.section('7. Semantic Traceability');

    if (!semantic) {
      pdf.paragraph('Semantic traceability unavailable.');
      return;
    }

    pdf.field('Status', semantic.status);
    pdf.field('Evaluated claims', semantic.evaluatedClaims);
    pdf.field('Semantic alerts', semantic.alertCount);
    pdf.field('Evidence gaps', semantic.evidenceGaps);

    pdf.field(
      'Potential contradictions',
      semantic.potentialContradictions
    );

    pdf.field(
      'Unique alerted fields',
      semantic.uniqueAlertedFields
    );

    pdf.field(
      'Changed alerted fields',
      semantic.changedAlertedFields
    );

    pdf.field(
      'Confirmed semantic issues',
      semantic.confirmedSemanticIssues
    );

    pdf.field(
      'Confirmed resolved semantic issues',
      semantic.confirmedResolvedSemanticIssues
    );

    pdf.field(
      'Semantic resolution rate (source value)',
      semantic.semanticIssueResolutionRate
    );

    pdf.paragraph(
      'A changed field is not proof of semantic resolution.',
      { bold: true }
    );

    if (!semantic.alerts?.length) {
      pdf.paragraph('No semantic alert details available.');
      return;
    }

    for (const [index, alert] of semantic.alerts.entries()) {

      const assessment = alert.assessment;

      pdf.ensureSpace(40);

      pdf.paragraph(
        `Semantic Alert ${index + 1}`,
        { bold: true, fontSize: 13 }
      );

      pdf.field('Model', alert.model);
      pdf.field('Status', alert.status);

      pdf.field(
        'Finding nature',
        assessment?.findingNature
      );

      pdf.field('Source field', assessment?.sourcePath);
      pdf.field('Reference ID', assessment?.referenceId);
      pdf.field('Statement ID', assessment?.statementId);
      pdf.field('Relation', assessment?.relation);

      pdf.field(
        'Source field changed',
        this.formatBoolean(alert.sourceFieldChanged)
      );

      pdf.field('BEFORE - Flagged statement', alert.before);
      pdf.field('AFTER - Revised statement', alert.after);

      pdf.field('Evidence', assessment?.evidence);
      pdf.field('Premise', assessment?.premise);
      pdf.field('Hypothesis', assessment?.hypothesis);
    }
  }

  private renderCurrentDashboard(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    pdf.section('8. Current Dashboard Metrics');

    const dashboard = data.currentDashboard;

    if (!dashboard) {
      pdf.paragraph('Current dashboard unavailable.');
      return;
    }

    const score = dashboard.score;
    const sonar = dashboard.sonar;

    pdf.paragraph(
      'Source: current release dashboard. These metrics ' +
      'are independent of the historical AI workflow.',
      { bold: true }
    );

    pdf.field('Dashboard release ID', dashboard.releaseId);
    pdf.field('Dashboard captured at', dashboard.capturedAt);

    const metrics = [
      ['Current official QIS', dashboard.finalQis],
      ['Business QIS', score.qualityIntelligenceScore],
      ['Coverage Score', score.coverageScore],
      ['Current Defect Quality Score', score.defectScore],
      ['Feedback Score', score.feedbackScore],
      ['Incident Score', score.incidentScore],
      ['RCA Score', score.rcaScore],
      ['Sonar Quality Score', sonar.sonarScore]
    ] as const;

    for (const [label, value] of metrics) {
      pdf.field(label, this.formatNumber(value, ' / 100'));
    }

    pdf.paragraph('Requirements and defects', {
      bold: true, fontSize: 12
    });

    pdf.field('Total requirements', score.totalRequirements);
    pdf.field('Covered requirements', score.coveredRequirements);
    pdf.field('Uncovered requirements', score.uncoveredRequirements);
    pdf.field('Total user stories', score.totalStories);
    pdf.field('Total bugs', score.totalBugs);

    pdf.field(
      'Current bugs per 100 user stories',
      this.formatNumber(score.bugRatio)
    );

    pdf.paragraph('Incidents and RCA', {
      bold: true, fontSize: 12
    });

    pdf.field('Total incidents', score.totalIncidents);
    pdf.field('Critical client bugs', score.criticalClientBugs);
    pdf.field('Most impacted client', score.mostImpactedClient);
    pdf.field('Incidents with RCA', score.incidentsWithRca);

    pdf.field(
      'RCA coverage',
      this.formatNumber(score.rcaCoverage, '%')
    );

    pdf.field(
      'Corrective action coverage',
      this.formatNumber(score.correctiveActionCoverage, '%')
    );

    pdf.field(
      'Preventive action coverage',
      this.formatNumber(score.preventiveActionCoverage, '%')
    );

    pdf.field(
      'Closure rate',
      this.formatNumber(score.closureRate, '%')
    );

    pdf.field('RCA done', score.rcaDone);
    pdf.field('RCA in progress', score.rcaInProgress);
    pdf.field('RCA to do', score.rcaToDo);

    pdf.paragraph('Sonar quality and contributions', {
      bold: true, fontSize: 12
    });

    pdf.field('Sonar Quality Gate', sonar.sonarQualityGate);
    pdf.field('Sonar bugs', sonar.bugs);
    pdf.field('Sonar vulnerabilities', sonar.vulnerabilities);
    pdf.field('Sonar code smells', sonar.codeSmells);

    pdf.field(
      'Sonar coverage',
      this.formatNumber(sonar.coverage, '%')
    );

    pdf.field(
      'Duplicated lines density',
      this.formatNumber(sonar.duplicatedLinesDensity, '%')
    );

    pdf.field(
      'Official Sonar weight',
      this.formatNumber(sonar.sonarWeight * 100, '%')
    );

    pdf.field(
      'Business QIS contribution',
      this.formatNumber(sonar.baseQisContribution)
    );

    pdf.field(
      'Sonar QIS contribution',
      this.formatNumber(sonar.sonarContribution)
    );

    if (sonar.sonarScore == null) {
      pdf.paragraph(
        'Sonar score unavailable. The official Java fallback ' +
        'is preserved without inventing a Sonar score.',
        { bold: true }
      );
    }
  }

  private renderWhatIfSimulation(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    pdf.section('9. What-if Simulation');

    const simulation = data.simulation;

    if (!simulation) {
      pdf.paragraph('What-if simulation unavailable.');
      return;
    }

    pdf.paragraph(
      'HYPOTHETICAL SIMULATION ONLY. This projection does not ' +
      'modify official metrics or authorize a release.',
      { bold: true }
    );

    pdf.field('Simulation release ID', simulation.releaseId);
    pdf.field('Simulation captured at', simulation.capturedAt);

    pdf.field(
      'Simulation baseline QIS',
      this.formatNumber(simulation.currentQis)
    );

    pdf.field(
      'Simulated QIS',
      this.formatNumber(simulation.simulatedQis)
    );

    pdf.field(
      'Simulated QIS difference',
      this.formatNumber(simulation.difference, ' points')
    );

    pdf.field(
      'Baseline threshold decision',
      simulation.currentDecision
    );

    pdf.field(
      'Simulated threshold decision',
      simulation.simulatedDecision
    );

    pdf.field(
      'Target QIS',
      this.formatNumber(simulation.targetQis)
    );

    pdf.field(
      'Baseline gap to GO',
      this.formatNumber(simulation.gapToGo, ' points')
    );

    const dimensions = [
      ['coverage', 'Coverage'],
      ['defect', 'Defect Quality'],
      ['feedback', 'Feedback'],
      ['incident', 'Incident'],
      ['rca', 'RCA'],
      ['sonar', 'Sonar Quality']
    ] as const;

    for (const [key, label] of dimensions) {

      pdf.ensureSpace(55);

      pdf.paragraph(label, {
        bold: true, fontSize: 12
      });

      pdf.field(
        'Current value',
        this.formatNumber(simulation.currentValues[key])
      );

      pdf.field(
        'Simulated value',
        this.formatNumber(simulation.simulatedValues[key])
      );

      pdf.field(
        'Simulation weight',
        this.formatNumber(simulation.weights[key], '%')
      );
    }

    pdf.paragraph('What-if recommendation', {
      bold: true, fontSize: 12
    });

    const recommendation = simulation.recommendation;

    if (!recommendation) {
      pdf.paragraph(
        'No single-dimension recommendation available.'
      );
      return;
    }

    pdf.field('Recommended metric', recommendation.metric);

    pdf.field(
      'Recommendation baseline value',
      this.formatNumber(recommendation.currentValue)
    );

    pdf.field(
      'Recommended target value',
      this.formatNumber(recommendation.targetValue)
    );

    pdf.field(
      'Required improvement',
      this.formatNumber(recommendation.improvement, ' points')
    );

    pdf.field(
      'Recommended projected QIS',
      this.formatNumber(recommendation.projectedQis)
    );

    pdf.paragraph(
      'This recommendation is a theoretical single-dimension ' +
      'projection from the baseline, not a verified outcome.'
    );
  }

  private renderExportWarnings(
    pdf: PdfLayoutEngine,
    warnings: string[]
  ): void {

    pdf.section('10. Export Validation and Limitations');

    pdf.field('Export validation status', 'VALID');

    if (warnings.length === 0) {
      pdf.paragraph('No export validation warnings.');
    } else {
      for (const warning of warnings) {
        pdf.paragraph(warning);
      }
    }

    pdf.paragraph(
      'Export validation checks data structure, source identity ' +
      'and supported mathematical consistency. It does not ' +
      'certify semantic correctness or release readiness.',
      { bold: true }
    );
  }

  private renderConclusion(
    pdf: PdfLayoutEngine,
    data: QualityReportExportData
  ): void {

    const report = data.historicalWorkflow.report;

    pdf.section('11. Release Conclusion');

    pdf.field(
      'Official Java recommendation',
      report.revisedAnalysis?.releaseDecision
    );

    pdf.field(
      'Human review required',
      this.formatBoolean(report.humanReviewRequired)
    );

    pdf.field(
      'Semantic quality certified',
      this.formatBoolean(report.semanticQualityCertified)
    );

    pdf.paragraph(
      'The final release decision remains the responsibility ' +
      'of authorized human stakeholders.',
      { bold: true }
    );

    pdf.paragraph(
      'Scope: historical AI workflow assessment, current dashboard ' +
      'metrics when available, and hypothetical What-if simulation ' +
      'when available. These sources must not be confused.'
    );
  }
}
