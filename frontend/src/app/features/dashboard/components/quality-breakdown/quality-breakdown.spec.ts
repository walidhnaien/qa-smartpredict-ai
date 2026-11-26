import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Chart } from 'chart.js';
import { QualityBreakdown } from './quality-breakdown';

const mocks = vi.hoisted(() => ({
  destroy: vi.fn()
}));

vi.mock('chart.js', () => ({
  registerables: [],
  Chart: class {
    static register = vi.fn();
    static created = vi.fn();

    destroy = mocks.destroy;

    constructor(canvas: unknown, config: unknown) {
      (this.constructor as any).created(canvas, config);
    }
  }
}));

describe('QualityBreakdown', () => {
  let fixture: ComponentFixture<QualityBreakdown>;
  const created = (Chart as any).created;

  beforeEach(async () => {
    created.mockClear();
    mocks.destroy.mockClear();

    await TestBed.configureTestingModule({
      imports: [QualityBreakdown]
    }).compileComponents();

    fixture = TestBed.createComponent(QualityBreakdown);
  });

  it('QB-01 creates a doughnut chart', () => {
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect(created).toHaveBeenCalledTimes(1);
    expect(created.mock.calls[0][1].type).toBe('doughnut');
  });

  it('QB-02 passes all six quality contributions', () => {
    fixture.componentRef.setInput('coverageContribution', 20);
    fixture.componentRef.setInput('defectContribution', 10);
    fixture.componentRef.setInput('feedbackContribution', 15);
    fixture.componentRef.setInput('incidentContribution', 8);
    fixture.componentRef.setInput('rcaContribution', 12);
    fixture.componentRef.setInput('sonarContribution', 5);

    fixture.detectChanges();

    const config = created.mock.calls[0][1];

    expect(config.data.labels).toEqual([
      'Coverage', 'Defect', 'Feedback',
      'Incident', 'RCA', 'Sonar'
    ]);

    expect(config.data.datasets[0].data).toEqual([
      20, 10, 15, 8, 12, 5
    ]);
  });

  it('QB-03 rebuilds the chart when inputs change', () => {
    fixture.detectChanges();

    fixture.componentRef.setInput('defectContribution', 25);
    fixture.detectChanges();

    expect(mocks.destroy).toHaveBeenCalledTimes(1);
    expect(created).toHaveBeenCalledTimes(2);

    const config = created.mock.calls[1][1];
    expect(config.data.datasets[0].data[1]).toBe(25);
  });

  it('QB-04 destroys the chart on component destruction', () => {
    fixture.detectChanges();
    fixture.destroy();

    expect(mocks.destroy).toHaveBeenCalledTimes(1);
  });
});
