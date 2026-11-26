import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { QUALITY_CHART_FACTORY } from './quality-chart.factory';
import type { ChartConfiguration } from 'chart.js';
import { QualityBreakdown } from './quality-breakdown';

const mocks = {
  destroy: vi.fn()
};

const created = vi.fn(
  (_canvas: HTMLCanvasElement,
   _config: ChartConfiguration<'doughnut'>) => ({
    destroy: mocks.destroy
  })
);

describe('QualityBreakdown', () => {
  let fixture: ComponentFixture<QualityBreakdown>;


  beforeEach(async () => {
    created.mockClear();
    mocks.destroy.mockClear();

    await TestBed.configureTestingModule({
      imports: [QualityBreakdown],
      providers: [
        {
          provide: QUALITY_CHART_FACTORY,
          useValue: created
        }
      ]
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
