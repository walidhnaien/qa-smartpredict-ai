import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  ChangeDetectorRef,
  Input,
  OnChanges,
  SimpleChanges
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
  implements AfterViewInit, OnDestroy, OnChanges {

  @Input()
  releaseId: string | null = null;
  @Input()
  refreshKey = 0;

  @ViewChild('timelineChart')
  chartCanvas!: ElementRef<HTMLCanvasElement>;

  history: QualitySnapshot[] = [];

  private chart?: Chart;

  loading = false;

  error = '';

  private viewInitialized = false;

  constructor(
    private historyService: QualityHistoryService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {

    this.viewInitialized = true;

    this.loadHistory();
  }

ngOnChanges(changes: SimpleChanges): void {

  if (!this.viewInitialized) {
    return;
  }

  if (
    changes['releaseId'] ||
    changes['refreshKey']
  ) {

    console.log(
      'TIMELINE REFRESH:',
      {
        releaseId: this.releaseId,
        refreshKey: this.refreshKey
      }
    );

    this.loadHistory();
  }
}

  ngOnDestroy(): void {

    this.chart?.destroy();
  }
  
  
loadHistory(): void {

  console.log(
    '=== TIMELINE LOAD HISTORY ==='
  );

  console.log(
    'Release ID received:',
    this.releaseId
  );

  this.chart?.destroy();
  this.chart = undefined;

  this.history = [];
  this.error = '';

  if (!this.releaseId) {

    console.warn(
      'TIMELINE: no releaseId received'
    );

    this.loading = false;

    return;
  }

  this.loading = true;

  const releaseId = this.releaseId;

  console.log(
    'Calling history API for:',
    releaseId
  );

  this.historyService
    .getHistoryByRelease(releaseId)
    .subscribe({

      next: (data: QualitySnapshot[]) => {

        console.log(
          'TIMELINE API RESPONSE:',
          data
        );

        console.log(
          'TIMELINE SNAPSHOT COUNT:',
          data.length
        );

        this.history = data;

        this.loading = false;

        this.cdr.detectChanges();

        if (this.history.length > 0) {

          setTimeout(() => {

            console.log(
              'Creating Timeline chart with',
              this.history.length,
              'snapshots'
            );

            this.createChart();

          }, 0);
        }
      },

      error: (err: unknown) => {

        console.error(
          'TIMELINE API ERROR:',
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
            data: this.history.map(
              item => item.qis
            ),
            tension: 0.3
          },

          {
            label: 'Coverage',
            data: this.history.map(
              item => item.coverageScore
            ),
            tension: 0.3
          },

          {
            label: 'Defect',
            data: this.history.map(
              item => item.defectScore
            ),
            tension: 0.3
          },

          {
            label: 'Sonar',
            data: this.history.map(
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