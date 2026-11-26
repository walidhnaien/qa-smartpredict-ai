import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WhatIfSimulator } from './what-if-simulator';

describe('WhatIfSimulator', () => {
  let component: WhatIfSimulator;
  let fixture: ComponentFixture<WhatIfSimulator>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhatIfSimulator],
    }).compileComponents();

    fixture = TestBed.createComponent(WhatIfSimulator);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });


  function ready(): void {

    Object.assign(component, {
      releaseId: 'A',
      contextGeneration: 1,
      sourceReady: true,

      currentQis: 70,

      coverage: 70,
      defect: 70,
      feedback: 70,
      incident: 70,
      rca: 70,
      sonar: 70,

      coverageWeight: 16,
      defectWeight: 16,
      feedbackWeight: 16,
      incidentWeight: 16,
      rcaWeight: 16,
      sonarWeight: 20
    });

    component.reset();
  }

  it('WSNAP-01 captures a valid simulation', () => {

    ready();

    const snapshot = component.captureSnapshot('A', 1);

    expect(snapshot?.releaseId).toBe('A');
    expect(snapshot?.currentQis).toBe(70);
    expect(snapshot?.simulatedQis).toBe(70);
    expect(snapshot?.difference).toBe(0);

  });

  it('WSNAP-02 captures modified simulated values', () => {

    ready();

    component.simulatedDefect = 100;

    const snapshot = component.captureSnapshot('A', 1);

    expect(snapshot?.simulatedValues.defect).toBe(100);
    expect(snapshot?.currentValues.defect).toBe(70);
    expect(snapshot?.simulatedQis).toBe(74.8);
    expect(snapshot?.difference).toBe(4.8);

  });

  it('WSNAP-03 preserves all six weights', () => {

    ready();

    expect(component.captureSnapshot('A', 1)?.weights)
      .toEqual({
        coverage: 16,
        defect: 16,
        feedback: 16,
        incident: 16,
        rca: 16,
        sonar: 20
      });

  });

  it('WSNAP-04 rejects another release', () => {

    ready();

    expect(component.captureSnapshot('B', 1))
      .toBeNull();

  });

  it('WSNAP-05 rejects an obsolete generation', () => {

    ready();

    expect(component.captureSnapshot('A', 2))
      .toBeNull();

  });

  it('WSNAP-06 rejects unavailable source data', () => {

    ready();
    component.sourceReady = false;

    expect(component.captureSnapshot('A', 1))
      .toBeNull();

  });

  it('WSNAP-07 rejects incomplete weights', () => {

    ready();
    component.defectWeight = 0;

    expect(component.captureSnapshot('A', 1))
      .toBeNull();

  });

  it('WSNAP-08 rejects invalid weights', () => {

    ready();

    for (const weight of [NaN, Infinity, -1, 101]) {

      component.defectWeight = weight;

      expect(component.captureSnapshot('A', 1))
        .toBeNull();

    }

  });

  it('WSNAP-09 rejects invalid simulated scores', () => {

    ready();

    for (const value of [NaN, Infinity, -1, 101]) {

      component.simulatedDefect = value;

      expect(component.captureSnapshot('A', 1))
        .toBeNull();

    }

  });

  it('WSNAP-10 preserves legitimate zero scores', () => {

    ready();

    component.defect = 0;
    component.simulatedDefect = 0;

    const snapshot = component.captureSnapshot('A', 1);

    expect(snapshot?.currentValues.defect).toBe(0);
    expect(snapshot?.simulatedValues.defect).toBe(0);

  });

  it('WSNAP-11 captures the existing recommendation', () => {

    ready();

    component.defect = 20;
    component.currentQis = 62;
    component.reset();

    const snapshot = component.captureSnapshot('A', 1);

    expect(snapshot?.recommendation)
      .toEqual(component.recommendation);

    expect(snapshot?.gapToGo).toBe(18);

  });

  it('WSNAP-12 preserves a null recommendation', () => {

    ready();

    component.currentQis = 85;

    expect(component.captureSnapshot('A', 1)?.recommendation)
      .toBeNull();

  });

  it('WSNAP-13 captures independent data', () => {

    ready();

    const snapshot = component.captureSnapshot('A', 1)!;

    component.simulatedDefect = 100;
    component.defectWeight = 25;

    expect(snapshot.simulatedValues.defect).toBe(70);
    expect(snapshot.weights.defect).toBe(16);

  });

  it('WSNAP-14 preserves official inputs during capture', () => {

    ready();

    component.simulatedDefect = 100;

    component.captureSnapshot('A', 1);

    expect(component.defect).toBe(70);
    expect(component.currentQis).toBe(70);
    expect(component.simulatedDefect).toBe(100);

  });

  it('WSNAP-15 captures simulated decision and target', () => {

    ready();

    component.simulatedCoverage = 100;
    component.simulatedDefect = 100;
    component.simulatedFeedback = 100;

    const snapshot = component.captureSnapshot('A', 1);

    expect(snapshot?.currentDecision).toBe('GO WITH RISK');
    expect(snapshot?.simulatedDecision).toBe('GO');
    expect(snapshot?.simulatedQis).toBe(84.4);
    expect(snapshot?.targetQis).toBe(80);
    expect(snapshot?.gapToGo).toBe(10);

  });

});
