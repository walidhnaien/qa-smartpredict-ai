import { Injectable, inject } from '@angular/core';

import {
  Observable,
  defer,
  forkJoin,
  map,
  timeout
} from 'rxjs';

import {
  AiOrchestratorService
} from '../ai-orchestrator.service';

import {
  Release
} from '../../models/release.model';

import {
  DashboardQualitySnapshot,
  WhatIfExportSnapshot,
  QualityReportExportData
} from '../../models/quality-report-export.model';

import {
  validateQualityReport
} from '../../models/ai-quality-report.validator';

import {
  validateQualityReportExport,
  ExportValidationContext
} from '../../models/quality-report-export.validator';

export interface SnapshotCollectionRequest {
  release: Release;
  runId: string;
  currentDashboard: DashboardQualitySnapshot | null;
  simulation: WhatIfExportSnapshot | null;
}

export interface SnapshotCollectionResult {
  data: QualityReportExportData;
  context: ExportValidationContext;
  warnings: string[];
}

@Injectable({
  providedIn: 'root'
})
export class QualityReportSnapshotCollector {

  private readonly api = inject(AiOrchestratorService);

  collect(
    request: SnapshotCollectionRequest
  ): Observable<SnapshotCollectionResult> {

    return defer(() => {

      // Capture all inputs before asynchronous retrieval.
      const captured = structuredClone(request);

      const releaseId = captured.release.id;
      const runId = captured.runId;

      if (!releaseId?.trim() || !runId?.trim()) {
        throw new Error(
          'Release ID and Run ID are required.'
        );
      }

      if (
        captured.currentDashboard &&
        captured.currentDashboard.releaseId !== releaseId
      ) {
        throw new Error(
          'Dashboard release mismatch.'
        );
      }

      if (
        captured.simulation &&
        captured.simulation.releaseId !== releaseId
      ) {
        throw new Error(
          'Simulation release mismatch.'
        );
      }

      return forkJoin({
        result: this.api.result(runId),
        steps: this.api.steps(runId).pipe(
          timeout(30000)
        )
      }).pipe(

        map(({ result, steps }) => {

          const checked = validateQualityReport(
            result,
            runId,
            releaseId
          );

          if (checked.status !== 'AVAILABLE') {
            throw new Error(
              'Historical AI report is invalid or unavailable.'
            );
          }

          const data: QualityReportExportData = {
            schemaVersion: '1.0',
            applicationVersion: '4.0.5',
            exportedAt: new Date().toISOString(),

            release: captured.release,

            historicalWorkflow: {
              report: checked.report,
              steps: steps
            },

            currentDashboard: captured.currentDashboard,
            simulation: captured.simulation
          };

          const context: ExportValidationContext = {
            expectedRunId: runId,
            expectedReleaseId: releaseId,
            stepsSourceRunId: runId
          };

          const validation =
            validateQualityReportExport(data, context);

          if (validation.status === 'INVALID') {
            throw new Error(
              validation.errors.join(' | ')
            );
          }

          return {
            data: structuredClone(data),
            context: structuredClone(context),
            warnings: [...validation.warnings]
          };

        })
      );

    });

  }

}
