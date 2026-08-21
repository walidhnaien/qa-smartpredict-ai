import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-quality-breakdown',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quality-breakdown.html',
  styleUrl: './quality-breakdown.css'
})
export class QualityBreakdown
  implements AfterViewInit, OnChanges, OnDestroy {

  @Input() coverageContribution = 0;
  @Input() defectContribution = 0;
  @Input() feedbackContribution = 0;
  @Input() incidentContribution = 0;
  @Input() sonarContribution = 0;

  @Input() finalQis = 0;

  @ViewChild('breakdownChart')
  chartCanvas!: ElementRef<HTMLCanvasElement>;

  private chart?: Chart;

  private viewInitialized = false;

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges): void {

    if (this.viewInitialized) {
      this.createChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private createChart(): void {

    if (!this.chartCanvas) {
      return;
    }

    this.chart?.destroy();

    const config: ChartConfiguration<'doughnut'> = {

      type: 'doughnut',

      data: {

        labels: [
          'Coverage',
          'Defect',
          'Feedback',
          'Incident',
          'Sonar'
        ],

        datasets: [{
          data: [
            this.coverageContribution,
            this.defectContribution,
            this.feedbackContribution,
            this.incidentContribution,
            this.sonarContribution
          ],

          borderWidth: 0
        }]
      },

      options: {

        responsive: true,
        maintainAspectRatio: false,

        cutout: '72%',

        plugins: {

          legend: {
            position: 'bottom'
          },

          tooltip: {
            callbacks: {

              label: context => {

                const value =
                  Number(context.raw ?? 0);

                return `${context.label}: ${value.toFixed(2)} points`;
              }
            }
          }
        }
      }
    };

    this.chart = new Chart(
      this.chartCanvas.nativeElement,
      config
    );
  }
}