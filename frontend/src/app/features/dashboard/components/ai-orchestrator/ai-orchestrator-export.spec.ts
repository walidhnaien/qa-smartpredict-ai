import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import { signal } from '@angular/core';

import {
  describe, it, expect,
  beforeEach, afterEach
} from 'vitest';

import { AiOrchestrator } from './ai-orchestrator';

import {
  AiQualityReport
} from '../ai-quality-report/ai-quality-report';

import {
  WorkflowRun
} from '../../../../core/models/ai-orchestrator.model';

import {
  AiQualityReport as Report,
  QualityReportState
} from '../../../../core/models/ai-quality-report.model';

describe('AI Orchestrator export readiness', () => {

  let component: AiOrchestrator;
  let http: HttpTestingController;

  const releaseId = 'synthetic-release';
  const runId = 'synthetic-run';

  const completedRun: WorkflowRun = {
    id: runId,
    release_id: releaseId,
    mode: 'REAL',
    status: 'COMPLETED',
    current_step: null,
    recovery_count: 0,
    duration_ms: 1000,
    error_message: null
  };

  function mockReport(
    state: QualityReportState = 'AVAILABLE',
    run: WorkflowRun = completedRun
  ) {
    const child = {
      runId,
      releaseId,
      state: signal<QualityReportState>(state),
      report: signal<Report | null>({
        run: { ...run }
      } as Report)
    };

    component['qualityReport'] =
      child as unknown as AiQualityReport;

    return child;
  }

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [AiOrchestrator],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);

    const fixture =
      TestBed.createComponent(AiOrchestrator);

    component = fixture.componentInstance;

    // Do not initialize the workflow or polling.
    component.releaseId = releaseId;
    component.run.set({ ...completedRun });

    mockReport();
  });

  afterEach(() => {
    http.expectNone(() => true);
    http.verify();
  });

  it('READY-01 accepts the matching completed report', () => {
    expect(
      component.getExportableRunId(releaseId)
    ).toBe(runId);
  });

  it('READY-02 rejects a missing release', () => {
    expect(
      component.getExportableRunId(null)
    ).toBeNull();
  });

  it('READY-03 rejects a different selected release', () => {
    expect(
      component.getExportableRunId('another-release')
    ).toBeNull();
  });

  it('READY-04 rejects a missing run', () => {
    component.run.set(null);

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-05 rejects a running workflow', () => {
    component.run.set({
      ...completedRun,
      status: 'RUNNING'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-06 rejects a pending workflow', () => {
    component.run.set({
      ...completedRun,
      status: 'PENDING'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-07 rejects a failed workflow', () => {
    component.run.set({
      ...completedRun,
      status: 'FAILED'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-08 rejects a non-REAL workflow', () => {
    component.run.set({
      ...completedRun,
      mode: 'MOCK'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-09 rejects a cross-release workflow', () => {
    component.run.set({
      ...completedRun,
      release_id: 'another-release'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-10 rejects a missing report component', () => {
    component['qualityReport'] = undefined;

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-11 rejects all unavailable report states', () => {

    const states: QualityReportState[] = [
      'IDLE', 'LOADING', 'UNAVAILABLE', 'ERROR'
    ];

    for (const state of states) {
      mockReport(state);

      expect(
        component.getExportableRunId(releaseId)
      ).toBeNull();
    }
  });

  it('READY-12 rejects a null report payload', () => {
    const child = mockReport();
    child.report.set(null);

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-13 rejects a mismatched report run', () => {
    mockReport('AVAILABLE', {
      ...completedRun,
      id: 'obsolete-run'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-14 rejects a mismatched report release', () => {
    mockReport('AVAILABLE', {
      ...completedRun,
      release_id: 'obsolete-release'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-15 rejects obsolete child inputs', () => {
    const child = mockReport();

    child.runId = 'obsolete-run';

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();

    child.runId = runId;
    child.releaseId = 'obsolete-release';

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-16 rejects export while orchestrator is busy', () => {
    component.busy.set(true);

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-17 allows historical export without preflight', () => {
    component.preflight.set(null);
    component.ready.set(false);

    expect(
      component.getExportableRunId(releaseId)
    ).toBe(runId);
  });

  it('READY-18 rejects export after destruction', () => {
    component.ngOnDestroy();

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-19 rejects an invalid report workflow status', () => {
    mockReport('AVAILABLE', {
      ...completedRun,
      status: 'RUNNING'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

  it('READY-20 rejects an invalid report workflow mode', () => {
    mockReport('AVAILABLE', {
      ...completedRun,
      mode: 'MOCK'
    });

    expect(
      component.getExportableRunId(releaseId)
    ).toBeNull();
  });

});
