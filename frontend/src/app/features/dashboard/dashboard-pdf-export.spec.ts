import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import {
  describe, it, expect,
  beforeEach, afterEach, vi
} from 'vitest';

import { NEVER } from 'rxjs';

import { Dashboard } from './dashboard';

import {
  AiOrchestrator
} from './components/ai-orchestrator/ai-orchestrator';

import {
  QualityReportSnapshotCollector
} from '../../core/services/pdf/quality-report-snapshot-collector.service';

import {
  QualityPdfReportService
} from '../../core/services/pdf/quality-pdf-report.service';

import {
  createDashboardSnapshot,
  createSimulationSnapshot
} from '../../core/models/testing/quality-report-export.fixture';

import {
  WorkflowStep
} from '../../core/models/ai-orchestrator.model';

import fixtureData
  from '../../core/models/testing/quality-report.fixture.json';

describe('Dashboard PDF export integration', () => {

  let component: Dashboard;
  let http: HttpTestingController;
  let collector: QualityReportSnapshotCollector;
  let pdfService: QualityPdfReportService;

  let activeRunId: string | null;

  const releaseId = fixtureData.run.release_id;
  const runId = fixtureData.run.id;

  const baseUrl =
    'http://localhost:8081/api/ai/orchestrator';

  const resultUrl =
    `${baseUrl}/run/${runId}/result`;

  const stepsUrl =
    `${baseUrl}/run/${runId}/steps`;

  const save = vi.fn();

  function report() {
    return structuredClone(fixtureData);
  }

  function steps(): WorkflowStep[] {
    return [{
      step_order: 1,
      step_name: 'COLLECT_METRICS',
      status: 'SUCCESS',
      model: 'test-model',
      attempt: 1,
      duration_ms: 1000,
      error_message: null
    } as WorkflowStep];
  }

  function pendingRequests() {

    const result = http.expectOne(req =>
      req.method === 'GET' &&
      req.url === resultUrl
    );

    const workflowSteps = http.expectOne(req =>
      req.method === 'GET' &&
      req.url === stepsUrl
    );

    return { result, workflowSteps };
  }

  function completeExport(): void {

    const requests = pendingRequests();

    requests.result.flush(report());
    requests.workflowSteps.flush(steps());
  }

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);

    collector = TestBed.inject(
      QualityReportSnapshotCollector
    );

    pdfService = TestBed.inject(
      QualityPdfReportService
    );

    const fixture =
      TestBed.createComponent(Dashboard);

    component = fixture.componentInstance;

    activeRunId = runId;

    component.releases = [{
      id: releaseId,
      name: 'Synthetic QA Release',
      version: '202601.0.0-TEST',
      type: 'MAJOR',
      status: 'READY_FOR_DECISION'
    }];

    component.selectedReleaseId = releaseId;

    component['aiOrchestrator'] = {
      getExportableRunId: (id: string | null) =>
        id === releaseId ? activeRunId : null
    } as unknown as AiOrchestrator;

    vi.spyOn(
      component,
      'captureDashboardSnapshot'
    ).mockImplementation(() =>
      createDashboardSnapshot(releaseId)
    );

    vi.spyOn(
      component,
      'captureWhatIfSnapshot'
    ).mockImplementation(() =>
      createSimulationSnapshot(releaseId)
    );

    vi.spyOn(
      component['cdr'],
      'detectChanges'
    ).mockImplementation(() => {});

    vi.spyOn(
      console,
      'error'
    ).mockImplementation(() => {});

    save.mockReset();

    // Generate a real PDF but intercept the download.
    const generate =
      pdfService.generate.bind(pdfService);

    vi.spyOn(
      pdfService,
      'generate'
    ).mockImplementation((data, context) => {

      const pdf = generate(data, context);

      vi.spyOn(pdf, 'save')
        .mockImplementation(save);

      return pdf;
    });

    vi.spyOn(collector, 'collect');

  });

  afterEach(() => {

    http.expectNone(req =>
      req.method !== 'GET'
    );

    http.verify();

    vi.restoreAllMocks();
  });

  it('INT-01 blocks export without an available report', () => {

    activeRunId = null;

    expect(component.canExportPdf).toBe(false);

    component.exportPdf();

    expect(collector.collect).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();

    http.expectNone(() => true);
  });

  it('INT-02 prevents duplicate exports', () => {

    component.exportPdf();

    expect(component.pdfExporting).toBe(true);
    expect(component.canExportPdf).toBe(false);

    component.exportPdf();

    expect(collector.collect).toHaveBeenCalledTimes(1);

    completeExport();

    expect(save).toHaveBeenCalledTimes(1);
    expect(component.pdfExporting).toBe(false);
  });

  it('INT-03 generates one real PDF for the selected run', () => {

    expect(component.canExportPdf).toBe(true);

    component.exportPdf();

    completeExport();

    expect(pdfService.generate)
      .toHaveBeenCalledTimes(1);

    expect(save).toHaveBeenCalledTimes(1);

    expect(save).toHaveBeenCalledWith(
      expect.stringMatching(
        /^QA-SmartPredict-Quality-Report-202601\.0\.0-TEST-\d{4}-\d{2}-\d{2}\.pdf$/
      )
    );

    expect(component.pdfExporting).toBe(false);
    expect(component.pdfExportError).toBe('');

    expect(component.pdfExportMessage)
      .toBe('PDF generated successfully.');

    expect(component.canExportPdf).toBe(true);
  });

  it('INT-04 cancels export when the release changes', () => {

    component.exportPdf();

    const requests = pendingRequests();

    vi.spyOn(
      component['service'],
      'getScoreByRelease'
    ).mockReturnValue(NEVER);

    component.selectedReleaseId = 'another-release';
    component.onReleaseChange();

    expect(requests.result.cancelled).toBe(true);
    expect(requests.workflowSteps.cancelled).toBe(true);

    expect(pdfService.generate).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();

    expect(component.pdfExporting).toBe(false);
  });

  it('INT-05 cancels export on dashboard destruction', () => {

    component.exportPdf();

    const requests = pendingRequests();

    component.ngOnDestroy();

    expect(requests.result.cancelled).toBe(true);
    expect(requests.workflowSteps.cancelled).toBe(true);

    expect(save).not.toHaveBeenCalled();
    expect(component.pdfExporting).toBe(false);
    expect(component.canExportPdf).toBe(false);
  });

  it('INT-06 rejects an obsolete Run ID', () => {

    component.exportPdf();

    activeRunId = 'another-run';

    completeExport();

    expect(pdfService.generate).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();

    expect(component.pdfExporting).toBe(false);
    expect(component.pdfExportMessage).toBe('');
  });

  it('INT-07 handles HTTP errors', () => {

    component.exportPdf();

    const requests = pendingRequests();

    requests.result.flush(
      { message: 'Unavailable' },
      {
        status: 500,
        statusText: 'Server Error'
      }
    );

    expect(requests.workflowSteps.cancelled).toBe(true);

    expect(component.pdfExportError)
      .toBe('PDF export failed. Please try again.');

    expect(component.pdfExporting).toBe(false);
    expect(save).not.toHaveBeenCalled();
  });

  it('INT-08 handles PDF generation exceptions', () => {

    vi.mocked(pdfService.generate)
      .mockImplementationOnce(() => {
        throw new Error('PDF generation failure');
      });

    component.exportPdf();

    completeExport();

    expect(component.pdfExportError)
      .toBe('PDF export failed. Please try again.');

    expect(component.pdfExporting).toBe(false);
    expect(save).not.toHaveBeenCalled();
  });

  it('INT-09 allows retry after failure', () => {

    vi.mocked(pdfService.generate)
      .mockImplementationOnce(() => {
        throw new Error('First attempt failed');
      });

    component.exportPdf();
    completeExport();

    expect(component.canExportPdf).toBe(true);
    expect(component.pdfExportError).not.toBe('');

    component.exportPdf();
    completeExport();

    expect(collector.collect).toHaveBeenCalledTimes(2);
    expect(save).toHaveBeenCalledTimes(1);

    expect(component.pdfExportError).toBe('');
    expect(component.pdfExporting).toBe(false);

    expect(component.pdfExportMessage)
      .toBe('PDF generated successfully.');
  });

  it('INT-10 uses only the expected read endpoints', () => {

    component.exportPdf();

    const requests = pendingRequests();

    http.expectNone(() => true);

    requests.result.flush(report());
    requests.workflowSteps.flush(steps());

    http.expectNone(() => true);

    expect(save).toHaveBeenCalledTimes(1);
  });

  it('INT-11 exports with explicit optional-data limitations', () => {

    vi.mocked(component.captureDashboardSnapshot)
      .mockReturnValue(null);

    component.exportPdf();

    completeExport();

    expect(component.captureWhatIfSnapshot)
      .not.toHaveBeenCalled();

    expect(pdfService.generate)
      .toHaveBeenCalledWith(
        expect.objectContaining({
          currentDashboard: null,
          simulation: null
        }),
        expect.objectContaining({
          expectedRunId: runId,
          expectedReleaseId: releaseId
        })
      );

    expect(save).toHaveBeenCalledTimes(1);

    expect(component.pdfExportMessage).toBe(
      'PDF generated with limitations. See the report warnings.'
    );
  });

  it('INT-12 preserves the original What-if snapshot', () => {

    const simulation =
      createSimulationSnapshot(releaseId);

    vi.mocked(component.captureWhatIfSnapshot)
      .mockReturnValue(simulation);

    component.exportPdf();

    simulation.simulatedQis = 95;
    simulation.simulatedValues.defect = 100;

    completeExport();

    expect(pdfService.generate)
      .toHaveBeenCalledWith(
        expect.objectContaining({
          simulation: expect.objectContaining({
            simulatedQis: 80,
            simulatedValues: expect.objectContaining({
              defect: 80
            })
          })
        }),
        expect.anything()
      );

    expect(save).toHaveBeenCalledTimes(1);
  });

  it('INT-13 cancels export on same-release refresh', () => {

    component.exportPdf();

    const requests = pendingRequests();

    vi.spyOn(
      component['service'],
      'getScoreByRelease'
    ).mockReturnValue(NEVER);

    component.loadSelectedRelease();

    expect(component.selectedReleaseId).toBe(releaseId);

    expect(requests.result.cancelled).toBe(true);
    expect(requests.workflowSteps.cancelled).toBe(true);

    expect(save).not.toHaveBeenCalled();
    expect(component.pdfExporting).toBe(false);
  });

  it('INT-14 handles download exceptions', () => {

    save.mockImplementationOnce(() => {
      throw new Error('Download failure');
    });

    component.exportPdf();

    completeExport();

    expect(save).toHaveBeenCalledTimes(1);

    expect(component.pdfExportError)
      .toBe('PDF export failed. Please try again.');

    expect(component.pdfExporting).toBe(false);
    expect(component.canExportPdf).toBe(true);
    expect(component.pdfExportMessage).toBe('');
  });

  it('INT-15 handles synchronous snapshot exceptions', () => {

    vi.mocked(component.captureDashboardSnapshot)
      .mockImplementationOnce(() => {
        throw new Error('Snapshot failure');
      });

    component.exportPdf();

    expect(collector.collect).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();

    expect(component.pdfExportError)
      .toBe('PDF export failed. Please try again.');

    expect(component.pdfExporting).toBe(false);
    expect(component.canExportPdf).toBe(true);

    http.expectNone(() => true);
  });

});
