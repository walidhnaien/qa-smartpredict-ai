import {
  createDashboardSnapshot,
  createSimulationSnapshot
} from '../../models/testing/quality-report-export.fixture';

import { TestBed } from '@angular/core/testing';

import {
  provideHttpClient
} from '@angular/common/http';

import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach
} from 'vitest';

import {
  QualityReportSnapshotCollector,
  SnapshotCollectionRequest,
  SnapshotCollectionResult
} from './quality-report-snapshot-collector.service';

import {
  AiQualityReport
} from '../../models/ai-quality-report.model';

import {
  DashboardQualitySnapshot,
  WhatIfExportSnapshot
} from '../../models/quality-report-export.model';

import fixture from '../../models/testing/quality-report.fixture.json';

describe('QualityReportSnapshotCollector', () => {

  let collector: QualityReportSnapshotCollector;
  let http: HttpTestingController;

  let output: SnapshotCollectionResult | undefined;
  let failure: unknown;

  const base =
    'http://localhost:8081/api/ai/orchestrator';

  function report(): AiQualityReport {
    return structuredClone(fixture) as unknown as AiQualityReport;
  }

  function request(): SnapshotCollectionRequest {

    const r = report();

    return {
      release: {
        id: r.run.release_id,
        name: 'Synthetic QA Release',
        version: '202601.0.0-TEST',
        type: 'MAJOR',
        status: 'READY_FOR_DECISION'
      },
      runId: r.run.id,
      currentDashboard: null,
      simulation: null
    };
  }

  function collect(input = request()) {

    return collector.collect(input).subscribe({
      next: value => output = value,
      error: error => failure = error
    });
  }

  function pending(input = request()) {

    const result = http.expectOne(
      `${base}/run/${input.runId}/result`
    );

    const steps = http.expectOne(
      `${base}/run/${input.runId}/steps`
    );

    expect(result.request.method).toBe('GET');
    expect(steps.request.method).toBe('GET');

    return { result, steps };
  }

  function step(order = 1) {
    return {
      step_order: order,
      step_name: 'COLLECT_METRICS',
      status: 'SUCCESS',
      model: null,
      attempt: 1,
      duration_ms: 1000,
      error_message: null
    };
  }

  function complete(
    value = report(),
    steps = [step()]
  ) {

    const p = pending();

    p.result.flush(value);
    p.steps.flush(steps);
  }

  function expectFailure(message: string) {

    expect(output).toBeUndefined();
    expect(failure).toBeInstanceOf(Error);
    expect((failure as Error).message)
      .toContain(message);
  }

  beforeEach(() => {

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    collector = TestBed.inject(
      QualityReportSnapshotCollector
    );

    http = TestBed.inject(
      HttpTestingController
    );

    output = undefined;
    failure = undefined;
  });

  afterEach(() => {

    http.expectNone(
      req => req.method === 'POST'
    );

    http.verify();
  });

  it('SNAP-01 collects a valid historical report', () => {

    collect();

    const p = pending();

    p.result.flush(report());

    expect(output).toBeUndefined();

    p.steps.flush([step()]);

    expect(failure).toBeUndefined();
    expect(output?.data.schemaVersion).toBe('1.0');
    expect(output?.data.applicationVersion).toBe('4.0.5');
    expect(output?.data.historicalWorkflow.report)
      .toEqual(report());

    expect(output?.data.historicalWorkflow.steps)
      .toHaveLength(1);

    expect(output?.data.historicalWorkflow.report
      .humanReviewRequired).toBe(true);

    expect(output?.data.historicalWorkflow.report
      .semanticQualityCertified).toBe(false);
  });

  it('SNAP-02 rejects a different run ID', () => {

    const r = report();
    r.run.id = 'different-run';

    collect();
    complete(r);

    expectFailure('Historical AI report');
  });

  it('SNAP-03 rejects a different release ID', () => {

    const r = report();
    r.run.release_id = 'different-release';

    collect();
    complete(r);

    expectFailure('Historical AI report');
  });

  it('SNAP-04 rejects a running workflow', () => {

    const r = report();
    r.run.status = 'RUNNING';

    collect();
    complete(r);

    expectFailure('Historical AI report');
  });

  it('SNAP-05 warns when dashboard is unavailable', () => {

    collect();
    complete();

    expect(output?.data.currentDashboard).toBeNull();

    expect(output?.warnings)
      .toContain('Current dashboard unavailable.');
  });

  it('SNAP-06 warns when simulation is unavailable', () => {

    collect();
    complete();

    expect(output?.data.simulation).toBeNull();

    expect(output?.warnings)
      .toContain('What-if simulation unavailable.');
  });

  it('SNAP-07 rejects a dashboard from another release', () => {

    const input = request();

    input.currentDashboard = {
      releaseId: 'different-release'
    } as DashboardQualitySnapshot;

    collect(input);

    expectFailure('Dashboard release mismatch.');

    http.expectNone(() => true);
  });

  it('SNAP-08 rejects a simulation from another release', () => {

    const input = request();

    input.simulation = {
      releaseId: 'different-release'
    } as WhatIfExportSnapshot;

    collect(input);

    expectFailure('Simulation release mismatch.');

    http.expectNone(() => true);
  });

  it('SNAP-09 preserves inputs captured at subscription', () => {

    const input = request();

    const original = structuredClone(input);

    collect(input);

    input.release.name = 'Modified release';
    input.release.id = 'different-release';
    input.runId = 'different-run';

    complete();

    expect(output?.data.release)
      .toEqual(original.release);

    expect(output?.context.expectedRunId)
      .toBe(original.runId);

    expect(output?.context.expectedReleaseId)
      .toBe(original.release.id);
  });

  it('SNAP-10 propagates result HTTP errors', () => {

    collect();

    const p = pending();

    p.result.flush('Server error', {
      status: 500,
      statusText: 'Server Error'
    });

    expect(output).toBeUndefined();

    expect(failure).toMatchObject({
      status: 500
    });

    expect(p.steps.cancelled).toBe(true);
  });

  it('SNAP-11 propagates steps HTTP errors', () => {

    collect();

    const p = pending();

    p.steps.flush('Server error', {
      status: 503,
      statusText: 'Service Unavailable'
    });

    expect(output).toBeUndefined();

    expect(failure).toMatchObject({
      status: 503
    });

    expect(p.result.cancelled).toBe(true);
  });

  it('SNAP-12 warns when workflow steps are empty', () => {

    collect();
    complete(report(), []);

    expect(output?.data.historicalWorkflow.steps)
      .toEqual([]);

    expect(output?.warnings)
      .toContain('Workflow steps unavailable.');
  });

  it('SNAP-13 rejects duplicate step orders', () => {

    collect();

    complete(report(), [
      step(1),
      step(1)
    ]);

    expectFailure('Duplicate workflow step order.');
  });

  it('SNAP-14 cancels pending requests on unsubscribe', () => {

    const subscription = collect();

    const p = pending();

    subscription.unsubscribe();

    expect(p.result.cancelled).toBe(true);
    expect(p.steps.cancelled).toBe(true);

    expect(output).toBeUndefined();
    expect(failure).toBeUndefined();
  });

  it('SNAP-15 rejects an empty run ID', () => {

    const input = request();
    input.runId = ' ';

    collect(input);

    expectFailure('Release ID and Run ID are required.');

    http.expectNone(() => true);
  });

  it('SNAP-16 returns the independent validation context', () => {

    const input = request();

    collect(input);
    complete();

    expect(output?.context).toEqual({
      expectedRunId: input.runId,
      expectedReleaseId: input.release.id,
      stepsSourceRunId: input.runId
    });

    expect(
      Number.isFinite(
        Date.parse(output?.data.exportedAt ?? '')
      )
    ).toBe(true);
  });


  it('SNAP-17 collects coherent dashboard and simulation', () => {
    const input = request();

    input.currentDashboard =
      createDashboardSnapshot(input.release.id);

    input.simulation =
      createSimulationSnapshot(input.release.id);

    collect(input);
    complete();

    expect(failure).toBeUndefined();
    expect(output?.warnings).toEqual([]);
    expect(output?.data.currentDashboard?.finalQis).toBe(72);
    expect(output?.data.simulation?.simulatedQis).toBe(80);
  });

  it('SNAP-18 blocks invalid dashboard mathematics', () => {
    const input = request();

    input.currentDashboard =
      createDashboardSnapshot(input.release.id);

    input.currentDashboard.sonar.sonarContribution = 99;

    collect(input);
    complete();

    expectFailure('Invalid Sonar contribution.');
  });

  it('SNAP-19 blocks invalid simulation mathematics', () => {
    const input = request();

    input.currentDashboard =
      createDashboardSnapshot(input.release.id);

    input.simulation =
      createSimulationSnapshot(input.release.id);

    input.simulation.simulatedQis = 95;

    collect(input);
    complete();

    expectFailure('Invalid simulated QIS calculation.');
  });

  it('SNAP-20 preserves snapshots during asynchronous retrieval', () => {
    const input = request();

    input.currentDashboard =
      createDashboardSnapshot(input.release.id);

    input.simulation =
      createSimulationSnapshot(input.release.id);

    const original = structuredClone(input);

    collect(input);

    input.currentDashboard.finalQis = 99;
    input.simulation.simulatedValues.defect = 0;

    complete();

    expect(failure).toBeUndefined();

    expect(output?.data.currentDashboard)
      .toEqual(original.currentDashboard);

    expect(output?.data.simulation)
      .toEqual(original.simulation);
  });

});
