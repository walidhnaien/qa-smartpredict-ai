import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';

import { provideHttpClient } from '@angular/common/http';

import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
  vi
} from 'vitest';

import { AiQualityReport } from './ai-quality-report';

import baseline from '../../../../core/models/testing/quality-report.fixture.json';

describe('AiQualityReport Component', () => {

  let fixture: ComponentFixture<AiQualityReport>;
  let component: AiQualityReport;
  let http: HttpTestingController;

  const runId = baseline.run.id;
  const releaseId = baseline.run.release_id;

  const baseUrl =
    'http://localhost:8081/api/ai/orchestrator/run';

  const reportUrl = `${baseUrl}/${runId}/result`;

  const data = (): any => structuredClone(baseline);

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [AiQualityReport],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AiQualityReport);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    http.expectNone(req => req.method === 'POST');
    http.verify();
  });

  function select(
    run: string | null = runId,
    release: string | null = releaseId
  ) {
    fixture.componentRef.setInput('runId', run);
    fixture.componentRef.setInput('releaseId', release);
    fixture.detectChanges();
  }

  function renderReport(report: any = data()) {
    select();
    http.expectOne(reportUrl).flush(report);
    fixture.detectChanges();
  }

  function pageText(): string {
    return fixture.nativeElement.textContent
      .replace(/\s+/g, ' ')
      .trim();
  }

  function metric(label: string): string | undefined {
    const elements = Array.from(
      fixture.nativeElement.querySelectorAll('.metric')
    ) as HTMLElement[];

    return elements.find(element =>
      element.querySelector('small')?.textContent?.trim()
        === label
    )?.querySelector('strong')?.textContent?.trim();
  }

  function metadata(label: string): string | undefined {
    const elements = Array.from(
      fixture.nativeElement.querySelectorAll('.metadata > div')
    ) as HTMLElement[];

    return elements.find(element =>
      element.querySelector('strong')?.textContent?.trim()
        === label
    )?.querySelector('span')?.textContent?.trim();
  }

  it('UI-01 remains IDLE without a run ID', () => {
    select(null, releaseId);

    expect(component.state()).toBe('IDLE');
    expect(component.report()).toBeNull();

    http.expectNone(() => true);
  });

  it('UI-02 displays LOADING during retrieval', () => {
    select();

    expect(component.state()).toBe('LOADING');
    expect(pageText()).toContain(
      'Loading completed workflow report'
    );

    http.expectOne(reportUrl).flush(data());
  });

  it('UI-03 displays a valid completed report', () => {
    renderReport();

    expect(component.state()).toBe('AVAILABLE');
    expect(component.report()?.run.id).toBe(runId);

    expect(metric('Quality Index Score')).toBe('68.89');
    expect(metric('Defect Quality Score')).toBe('22.22');
    expect(metric('Official risk level')).toBe('HIGH');

    expect(pageText()).toContain('GO_WITH_RISK');
  });

  it('UI-04 rejects an invalid report', () => {
    const report = data();
    report.run.id = 'incorrect-run';

    renderReport(report);

    expect(component.state()).toBe('ERROR');
    expect(component.report()).toBeNull();

    expect(pageText()).toContain(
      'Report identity or workflow status mismatch'
    );
  });

  it('UI-05 handles an incomplete report', () => {
    const report = data();
    delete report.revisedAnalysis;

    renderReport(report);

    expect(component.state()).toBe('UNAVAILABLE');
    expect(component.report()).toBeNull();

    expect(pageText()).toContain(
      'Report unavailable or incomplete'
    );
  });

  it('UI-06 retries after HTTP failure without starting AI', () => {
    select();

    http.expectOne(reportUrl).flush(null, {
      status: 500,
      statusText: 'Internal Server Error'
    });

    fixture.detectChanges();

    expect(component.state()).toBe('ERROR');

    const button = fixture.nativeElement.querySelector(
      'button'
    ) as HTMLButtonElement;

    expect(button.textContent).toContain(
      'Retry report retrieval'
    );

    button.click();

    expect(component.state()).toBe('LOADING');

    const retry = http.expectOne(reportUrl);

    expect(retry.request.method).toBe('GET');

    retry.flush(data());
    fixture.detectChanges();

    expect(component.state()).toBe('AVAILABLE');
  });

  it('UI-07 clears the previous report on release change', () => {
    renderReport();

    expect(component.state()).toBe('AVAILABLE');

    select('new-run', 'new-release');

    expect(component.report()).toBeNull();
    expect(component.state()).toBe('LOADING');
    expect(pageText()).not.toContain('GO_WITH_RISK');

    const report = data();
    report.run.id = 'new-run';
    report.run.release_id = 'new-release';

    http.expectOne(`${baseUrl}/new-run/result`).flush(report);

    fixture.detectChanges();

    expect(component.state()).toBe('AVAILABLE');
    expect(component.report()?.run.id).toBe('new-run');
  });

  it('UI-08 cancels obsolete requests after input changes', () => {
    select();

    const previous = http.expectOne(reportUrl);

    select('new-run', 'new-release');

    expect(previous.cancelled).toBe(true);

    const report = data();
    report.run.id = 'new-run';
    report.run.release_id = 'new-release';

    http.expectOne(`${baseUrl}/new-run/result`).flush(report);

    fixture.detectChanges();

    expect(component.state()).toBe('AVAILABLE');
    expect(component.report()?.run.id).toBe('new-run');
  });

  it('UI-09 cancels pending requests on destruction', () => {
    select();

    const pending = http.expectOne(reportUrl);

    fixture.destroy();

    expect(pending.cancelled).toBe(true);
  });

  it('UI-10 displays unavailable metrics as N/A', () => {
    const report = data();

    report.revisedAnalysis.qis = null;
    report.revisedAnalysis.confidence = null;

    renderReport(report);

    expect(component.state()).toBe('AVAILABLE');
    expect(metric('Quality Index Score')).toBe('N/A');
    expect(metric('Analysis confidence')).toBe('N/A');
  });

  it('UI-11 displays governance without semantic certification', () => {
    renderReport();

    expect(metadata('Human review required')).toBe('YES');
    expect(metadata('Semantic quality certified')).toBe('NO');
    expect(metadata('Guardrail status')).toBe('PASS_GUARDRAILS');
    expect(metadata('Semantic resolution rate')).toBe('N/A');

    expect(pageText()).toContain(
      'This report does not constitute release approval'
    );
  });

  it('UI-12 renders all five semantic alerts', () => {
    renderReport();

    const alerts = fixture.nativeElement.querySelectorAll(
      'details.semantic-alert'
    );

    expect(alerts.length).toBe(5);
    expect(metric('Evaluated claims')).toBe('6');
    expect(metric('Semantic alerts')).toBe('5');
    expect(metric('Evidence gaps')).toBe('4');
    expect(metric('Potential contradictions')).toBe('1');
    expect(metric('Changed alerted fields')).toBe('4');

    for (const alert of Array.from(alerts) as HTMLElement[]) {
      expect(alert.querySelector('.before')).not.toBeNull();
      expect(alert.querySelector('.after')).not.toBeNull();
    }
  });


  it('UI-13 handles HTTP 404 and allows retry', () => {
    select();

    http.expectOne(reportUrl).flush(null, {
      status: 404,
      statusText: 'Not Found'
    });

    fixture.detectChanges();

    expect(component.state()).toBe('UNAVAILABLE');
    expect(component.report()).toBeNull();

    const button = fixture.nativeElement.querySelector(
      'button'
    ) as HTMLButtonElement;

    expect(button.textContent).toContain(
      'Retry report retrieval'
    );

    button.click();

    const retry = http.expectOne(reportUrl);
    expect(retry.request.method).toBe('GET');

    retry.flush(data());
    fixture.detectChanges();

    expect(component.state()).toBe('AVAILABLE');
  });

  it('UI-14 handles timeout without starting AI', () => {
    vi.useFakeTimers();

    select();

    const pending = http.expectOne(reportUrl);

    vi.advanceTimersByTime(29999);
    expect(component.state()).toBe('LOADING');

    vi.advanceTimersByTime(1);

    expect(pending.cancelled).toBe(true);
    expect(component.state()).toBe('ERROR');
    expect(component.report()).toBeNull();

    vi.useRealTimers();

    fixture.detectChanges();

    expect(pageText()).toContain(
      'Retry report retrieval'
    );

    const button = fixture.nativeElement.querySelector(
      'button'
    ) as HTMLButtonElement;

    button.click();

    const retry = http.expectOne(reportUrl);
    expect(retry.request.method).toBe('GET');

    retry.flush(data());
    fixture.detectChanges();

    expect(component.state()).toBe('AVAILABLE');
  });

  it('UI-15 does not display an empty analysis', () => {
    const report = data();
    report.revisedAnalysis = {};

    renderReport(report);

    expect(component.state()).toBe('UNAVAILABLE');
    expect(component.report()).toBeNull();
    expect(pageText()).not.toContain('GO_WITH_RISK');
  });

});
