import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  Chart,
  ChartConfiguration,
  registerables
} from 'chart.js';

import { QualityHistoryService }
  from '../../../../core/services/quality-history.service';

import { QualitySnapshot }
  from '../../../../core/models/quality-snapshot.model';

Chart.register(...registerables);

@Component({
  selector: 'app-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline.html',
  styleUrl: './timeline.css'
})
export class Timeline
  implements AfterViewInit, OnDestroy {

  @ViewChild('timelineChart')
  chartCanvas!: ElementRef<HTMLCanvasElement>;

  history: QualitySnapshot[] = [];

  private chart?: Chart;

  loading = true;

  error = '';

  constructor(
    private historyService: QualityHistoryService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {

    this.loadHistory();
  }

  ngOnDestroy(): void {

    this.chart?.destroy();
  }

loadHistory(): void {

  this.loading = true;

  this.historyService
    .getHistory()
    .subscribe({

      next: data => {

        console.log('QUALITY HISTORY:', data);

        this.history = data;

        this.loading = false;

        // Rend le canvas après le *ngIf
        this.cdr.detectChanges();

        // Puis création du graphique
        this.createChart();
      },

      error: err => {

        console.error(
          'Erreur Quality History:',
          err
        );

        this.error =
          'Unable to load quality history';

        this.loading = false;

        this.cdr.detectChanges();
      }

    });
}
  private createChart(): void {

    if (!this.chartCanvas) {
      return;
    }

    this.chart?.destroy();

    const labels =
      this.history.map(item =>
        new Date(item.analysisDate)
          .toLocaleString()
      );

    const config:
      ChartConfiguration<'line'> = {

      type: 'line',

      data: {

        labels,

        datasets: [

          {
            label: 'QIS',
            data:
              this.history.map(
                item => item.qis
              ),
            tension: 0.3
          },

          {
            label: 'Coverage',
            data:
              this.history.map(
                item => item.coverageScore
              ),
            tension: 0.3
          },

          {
            label: 'Defect',
            data:
              this.history.map(
                item => item.defectScore
              ),
            tension: 0.3
          },

          {
            label: 'Sonar',
            data:
              this.history.map(
                item => item.sonarScore
              ),
            tension: 0.3
          }

        ]
      },

      options: {

        responsive: true,

        maintainAspectRatio: false,

        scales: {

          y: {
            beginAtZero: true,
            max: 100
          }

        },

        plugins: {

          legend: {
            position: 'bottom'
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