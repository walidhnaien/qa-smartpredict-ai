import { validateQualityReport } from '../../../../core/models/ai-quality-report.validator';
import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import {
  AiQualityReport as Report,
  QualityReportState
} from '../../../../core/models/ai-quality-report.model';

import {
  AiOrchestratorService
} from '../../../../core/services/ai-orchestrator.service';

@Component({
  selector: 'app-ai-quality-report',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-quality-report.html',
  styleUrl: './ai-quality-report.css'
})
export class AiQualityReport implements OnChanges, OnDestroy {

  @Input() runId: string | null = null;
  @Input() releaseId: string | null = null;

  private api = inject(AiOrchestratorService);
  private request?: Subscription;
  private generation = 0;

  report = signal<Report | null>(null);
  state = signal<QualityReportState>('IDLE');
  error = signal('');

  ngOnChanges(): void {
    this.load();
  }

  ngOnDestroy(): void {
    ++this.generation;
    this.request?.unsubscribe();
  }

  load(): void {
    const generation = ++this.generation;

    this.request?.unsubscribe();
    this.report.set(null);
    this.error.set('');

    const runId = this.runId;
    const releaseId = this.releaseId;

    if (!runId || !releaseId) {
      this.state.set('IDLE');
      return;
    }

    this.state.set('LOADING');

    this.request = this.api.result(runId).subscribe({
      next: result => {
        if (generation !== this.generation) return;

        const checked = validateQualityReport(
          result, runId, releaseId
        );

        this.state.set(checked.status);

        if (checked.status === 'ERROR') {
          this.error.set(checked.message);
          return;
        }

        if (checked.status === 'AVAILABLE') {
          this.report.set(checked.report);
        }
      },

      error: err => {
        if (generation !== this.generation) return;

        if (err?.status === 404) {
          this.state.set('UNAVAILABLE');
          return;
        }

        this.state.set('ERROR');
        this.error.set(
          'Unable to retrieve the report. Retry without starting a new workflow.'
        );
      }
    });
  }
}
