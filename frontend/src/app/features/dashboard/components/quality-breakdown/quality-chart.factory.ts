import { InjectionToken } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

export interface QualityChartHandle {
  destroy(): void;
}

export type QualityChartFactory = (
  canvas: HTMLCanvasElement,
  config: ChartConfiguration<'doughnut'>
) => QualityChartHandle;

export const QUALITY_CHART_FACTORY =
  new InjectionToken<QualityChartFactory>(
    'QUALITY_CHART_FACTORY',
    {
      providedIn: 'root',
      factory: () => {
        Chart.register(...registerables);
        return (canvas, config) => new Chart(canvas, config);
      }
    }
  );
