import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';
import {
  describe, it, expect, beforeEach, afterEach, vi
} from 'vitest';
import { NEVER } from 'rxjs';
import { jsPDF } from 'jspdf';

import { Dashboard } from './dashboard';
import { DashboardScreenshotService }
  from '../../core/services/pdf/dashboard-screenshot.service';
import { createDashboardSnapshot }
  from '../../core/models/testing/quality-report-export.fixture';

describe('Dashboard screenshot export', () => {

  let component: Dashboard;
  let service: DashboardScreenshotService;
  let http: HttpTestingController;
  let element: HTMLElement;
  let pdf: jsPDF;

  const releaseId = 'screen-test-release';
  const save = vi.fn();

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(DashboardScreenshotService);

    component =
      TestBed.createComponent(Dashboard).componentInstance;

    const snapshot = createDashboardSnapshot(releaseId);

    component.releases = [{
      id: releaseId,
      name: 'Screenshot Test Release',
      version: '202601.0.0-TEST',
      type: 'MAJOR',
      status: 'READY_FOR_DECISION'
    }];

    component.selectedReleaseId = releaseId;
    component['loadedReleaseId'] = releaseId;
    component.score = snapshot.score;
    component.qisSonar = snapshot.sonar;

    const dashboardElement =
      document.getElementById('quality-report');

    if (!dashboardElement) {
      throw new Error('Angular dashboard element not found');
    }

    element = dashboardElement;

    expect(
      document.querySelectorAll('#quality-report').length
    ).toBe(1);

    save.mockReset();

    pdf = new jsPDF();

    vi.spyOn(pdf, 'save').mockImplementation(save);
    vi.spyOn(service, 'generate').mockResolvedValue(pdf);

    vi.spyOn(component['cdr'], 'markForCheck')
      .mockImplementation(() => {});
  });

  afterEach(() => {
    element.remove();
    http.expectNone(() => true);
    http.verify();
    vi.restoreAllMocks();
  });

  it('SCREEN-01 exports without an AI report', async () => {

    expect(component.canExportPdf).toBe(false);
    expect(component.canExportDashboardPdf).toBe(true);

    await component.exportDashboardPdf();

    expect(service.generate).toHaveBeenCalledWith(element);
    expect(save).toHaveBeenCalledTimes(1);
    expect(component.screenshotExporting).toBe(false);
    expect(component.screenshotExportError).toBe('');
  });

  it('SCREEN-02 generates the expected filename', async () => {

    await component.exportDashboardPdf();

    expect(save).toHaveBeenCalledWith(
      expect.stringMatching(
        /^QA-SmartPredict-Dashboard-202601\.0\.0-TEST-\d{4}-\d{2}-\d{2}\.pdf$/
      )
    );
  });

  it('SCREEN-03 blocks export while loading', async () => {

    component.loadingRelease = true;

    expect(component.canExportDashboardPdf).toBe(false);

    await component.exportDashboardPdf();

    expect(service.generate).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();
  });

  it('SCREEN-04 blocks inconsistent release context', async () => {

    component['loadedReleaseId'] = 'another-release';

    expect(component.canExportDashboardPdf).toBe(false);

    await component.exportDashboardPdf();

    expect(service.generate).not.toHaveBeenCalled();
  });

  it('SCREEN-05 prevents concurrent screenshot exports', async () => {

    let resolve!: (value: jsPDF) => void;

    vi.mocked(service.generate).mockReturnValueOnce(
      new Promise<jsPDF>(r => { resolve = r; })
    );

    const pending = component.exportDashboardPdf();

    expect(component.screenshotExporting).toBe(true);
    expect(component.canExportDashboardPdf).toBe(false);
    expect(component.canExportPdf).toBe(false);

    await component.exportDashboardPdf();

    expect(service.generate).toHaveBeenCalledTimes(1);

    resolve(pdf);
    await pending;

    expect(save).toHaveBeenCalledTimes(1);
    expect(component.screenshotExporting).toBe(false);
  });

  it('SCREEN-06 blocks screenshot during structured export', async () => {

    component.pdfExporting = true;

    expect(component.canExportDashboardPdf).toBe(false);

    await component.exportDashboardPdf();

    expect(service.generate).not.toHaveBeenCalled();
  });

  it('SCREEN-07 suppresses download after release change', async () => {

    let resolve!: (value: jsPDF) => void;

    vi.mocked(service.generate).mockReturnValueOnce(
      new Promise<jsPDF>(r => { resolve = r; })
    );

    const pending = component.exportDashboardPdf();

    vi.spyOn(component['service'], 'getScoreByRelease')
      .mockReturnValue(NEVER);

    component.selectedReleaseId = 'another-release';
    component.onReleaseChange();

    resolve(pdf);
    await pending;

    expect(save).not.toHaveBeenCalled();
    expect(component.screenshotExporting).toBe(false);
  });

  it('SCREEN-08 suppresses download after destruction', async () => {

    let resolve!: (value: jsPDF) => void;

    vi.mocked(service.generate).mockReturnValueOnce(
      new Promise<jsPDF>(r => { resolve = r; })
    );

    const pending = component.exportDashboardPdf();

    component.ngOnDestroy();

    resolve(pdf);
    await pending;

    expect(save).not.toHaveBeenCalled();
    expect(component.canExportDashboardPdf).toBe(false);
  });

  it('SCREEN-09 handles capture failure and retry', async () => {

    vi.mocked(service.generate).mockRejectedValueOnce(
      new Error('Capture failure')
    );

    await component.exportDashboardPdf();

    expect(component.screenshotExportError).toBe(
      'Dashboard PDF export failed. Please try again.'
    );

    expect(component.screenshotExporting).toBe(false);
    expect(save).not.toHaveBeenCalled();
    expect(component.canExportDashboardPdf).toBe(true);

    await component.exportDashboardPdf();

    expect(save).toHaveBeenCalledTimes(1);
    expect(component.screenshotExportError).toBe('');
  });

  it('SCREEN-10 handles missing dashboard element', async () => {

    element.remove();

    expect(
      document.getElementById('quality-report')
    ).toBeNull();

    await component.exportDashboardPdf();

    expect(component.screenshotExportError).toBe(
      'Dashboard is unavailable.'
    );

    expect(service.generate).not.toHaveBeenCalled();
    expect(save).not.toHaveBeenCalled();
  });
});
