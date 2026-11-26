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

import { Subject } from 'rxjs';

import { Dashboard } from './dashboard';
import {
  QualityIntelligence
} from '../../core/models/quality-intelligence';
import {
  QisSonar
} from '../../core/models/qis-sonar.model';

describe('Dashboard release generation guard', () => {

  let component: Dashboard;
  let http: HttpTestingController;

  const score = (value: number) => ({
    qualityIntelligenceScore: value
  } as QualityIntelligence);

  const sonar = (value: number) => ({
    finalQis: value
  } as QisSonar);

  function select(releaseId: string | null): void {
    component.selectedReleaseId = releaseId;
    component.onReleaseChange();
  }

  function provenance() {
    return {
      releaseId: component['loadedReleaseId'],
      capturedAt: component['releaseMetricsCapturedAt']
    };
  }

  function mockScore() {
    const response = new Subject<QualityIntelligence>();

    vi.spyOn(
      component['service'],
      'getScoreByRelease'
    ).mockReturnValueOnce(response.asObservable());

    return response;
  }

  function mockSonar() {
    const response = new Subject<QisSonar>();

    vi.spyOn(
      component['sonarService'],
      'getQisWithSonarByRelease'
    ).mockReturnValueOnce(response.asObservable());

    return response;
  }

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(Dashboard);

    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);

    // Isolate the release loading logic.
    // Do not initialize the full dashboard.
    vi.spyOn(
      component['cdr'],
      'detectChanges'
    ).mockImplementation(() => {});

  });

  afterEach(() => {
    http.expectNone(req => req.method === 'POST');
    http.verify();
    vi.restoreAllMocks();
  });

  it('RACE-01 accepts the current release metrics', () => {

    const a = mockScore();
    const sa = mockSonar();

    select('A');

    expect(component.loadingRelease).toBe(true);

    a.next(score(65));
    sa.next(sonar(70));

    expect(component.score).toEqual(score(65));
    expect(component.qisSonar).toEqual(sonar(70));
    expect(component.loadingRelease).toBe(false);

  });

  it('RACE-02 ignores an obsolete business response', () => {

    const a = mockScore();
    select('A');

    const b = mockScore();
    const sb = mockSonar();

    select('B');

    b.next(score(80));
    sb.next(sonar(85));

    a.next(score(20));

    expect(component.score).toEqual(score(80));
    expect(component.qisSonar).toEqual(sonar(85));
    expect(provenance().releaseId).toBe('B');

  });

  it('RACE-03 ignores an obsolete Sonar response', () => {

    const a = mockScore();
    const sa = mockSonar();

    select('A');
    a.next(score(40));

    const b = mockScore();
    const sb = mockSonar();

    select('B');
    b.next(score(80));
    sb.next(sonar(85));

    sa.next(sonar(45));

    expect(component.score).toEqual(score(80));
    expect(component.qisSonar).toEqual(sonar(85));
    expect(provenance().releaseId).toBe('B');

  });

  it('RACE-04 ignores an obsolete HTTP error', () => {

    const a = mockScore();
    select('A');

    const b = mockScore();
    const sb = mockSonar();

    select('B');

    a.error(new Error('Obsolete error'));

    expect(component.loadingRelease).toBe(true);

    b.next(score(80));
    sb.next(sonar(85));

    expect(component.loadingRelease).toBe(false);
    expect(provenance().releaseId).toBe('B');

  });

  it('RACE-05 rejects an old A response after A-B-A', () => {

    const oldA = mockScore();
    select('A');

    const b = mockScore();
    select('B');

    const newA = mockScore();
    const sa = mockSonar();

    select('A');

    newA.next(score(90));
    sa.next(sonar(95));

    oldA.next(score(10));
    b.next(score(20));

    expect(component.score).toEqual(score(90));
    expect(component.qisSonar).toEqual(sonar(95));
    expect(provenance().releaseId).toBe('A');

  });

  it('RACE-06 accepts only the latest refresh', () => {

    const first = mockScore();
    select('A');

    const second = mockScore();
    const sa = mockSonar();

    component.loadSelectedRelease();

    second.next(score(90));
    sa.next(sonar(95));

    first.next(score(10));

    expect(component.score).toEqual(score(90));
    expect(component.qisSonar).toEqual(sonar(95));

  });

  it('RACE-07 invalidates data on release deselection', () => {

    const a = mockScore();
    const sa = mockSonar();

    select('A');
    a.next(score(60));

    select(null);

    sa.next(sonar(70));

    expect(component.score).toBeNull();
    expect(component.qisSonar).toBeNull();
    expect(component.loadingRelease).toBe(false);
    expect(provenance().releaseId).toBeNull();
    expect(provenance().capturedAt).toBeNull();

  });

  it('RACE-08 ignores responses after destruction', () => {

    const a = mockScore();

    select('A');

    component.ngOnDestroy();

    a.next(score(90));

    expect(component.score).toBeNull();
    expect(component.qisSonar).toBeNull();
    expect(provenance().releaseId).toBeNull();

  });

  it('RACE-09 does not certify provenance on Sonar error', () => {

    vi.spyOn(console, 'error')
      .mockImplementation(() => {});

    const a = mockScore();
    const sa = mockSonar();

    select('A');

    a.next(score(70));
    sa.error(new Error('Sonar unavailable'));

    expect(component.score).toEqual(score(70));
    expect(component.qisSonar).toBeNull();
    expect(component.loadingRelease).toBe(false);
    expect(provenance().releaseId).toBeNull();
    expect(provenance().capturedAt).toBeNull();

  });

  it('RACE-10 records provenance only after both responses', () => {

    const a = mockScore();
    const sa = mockSonar();

    select('A');

    expect(provenance().releaseId).toBeNull();

    a.next(score(70));

    expect(provenance().releaseId).toBeNull();
    expect(provenance().capturedAt).toBeNull();

    sa.next(sonar(75));

    expect(provenance().releaseId).toBe('A');
    expect(provenance().capturedAt).not.toBeNull();

    expect(
      Number.isFinite(
        Date.parse(provenance().capturedAt!)
      )
    ).toBe(true);

  });



  function completeRelease(
    releaseId = 'A',
    businessQis = 65,
    finalQis = 70
  ): void {

    const a = mockScore();
    const sa = mockSonar();

    select(releaseId);

    a.next(score(businessQis));
    sa.next(sonar(finalQis));
  }

  it('DSNAP-01 captures a completed dashboard', () => {

    completeRelease();

    const snapshot = component.captureDashboardSnapshot();

    expect(snapshot).not.toBeNull();
    expect(snapshot?.releaseId).toBe('A');
    expect(snapshot?.finalQis).toBe(70);
    expect(snapshot?.score).toEqual(score(65));
    expect(snapshot?.sonar).toEqual(sonar(70));
    expect(snapshot?.capturedAt)
      .toBe(provenance().capturedAt);

  });

  it('DSNAP-02 returns null without a selected release', () => {

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-03 rejects an incomplete business load', () => {

    mockScore();
    select('A');

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-04 rejects a pending Sonar response', () => {

    const a = mockScore();
    mockSonar();

    select('A');
    a.next(score(65));

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-05 rejects mismatched release provenance', () => {

    completeRelease('A');

    component.selectedReleaseId = 'B';

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-06 rejects a snapshot after deselection', () => {

    completeRelease();
    select(null);

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-07 rejects a snapshot after destruction', () => {

    completeRelease();

    component.ngOnDestroy();

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

  it('DSNAP-08 preserves an independent data copy', () => {

    completeRelease();

    const snapshot = component.captureDashboardSnapshot()!;

    component.score!.qualityIntelligenceScore = 10;
    component.qisSonar!.finalQis = 20;

    expect(snapshot.score.qualityIntelligenceScore)
      .toBe(65);

    expect(snapshot.sonar.finalQis).toBe(70);
    expect(snapshot.finalQis).toBe(70);

  });

  it('DSNAP-09 preserves a snapshot after release change', () => {

    completeRelease('A', 65, 70);

    const snapshot = component.captureDashboardSnapshot()!;

    completeRelease('B', 80, 85);

    expect(snapshot.releaseId).toBe('A');
    expect(snapshot.finalQis).toBe(70);

    const latest = component.captureDashboardSnapshot();

    expect(latest?.releaseId).toBe('B');
    expect(latest?.finalQis).toBe(85);

  });

  it('DSNAP-10 rejects invalid numeric QIS values', () => {

    completeRelease();

    for (const value of [
      NaN, Infinity, -Infinity, -1, 101
    ]) {

      component.qisSonar!.finalQis = value;

      expect(component.captureDashboardSnapshot())
        .toBeNull();
    }

  });

  it('DSNAP-11 preserves a legitimate zero QIS', () => {

    completeRelease('A', 0, 0);

    expect(
      component.captureDashboardSnapshot()?.finalQis
    ).toBe(0);

  });

  it('DSNAP-12 rejects capture after Sonar failure', () => {

    vi.spyOn(console, 'error')
      .mockImplementation(() => {});

    const a = mockScore();
    const sa = mockSonar();

    select('A');
    a.next(score(65));

    sa.error(new Error('Sonar unavailable'));

    expect(component.captureDashboardSnapshot())
      .toBeNull();

  });

});
