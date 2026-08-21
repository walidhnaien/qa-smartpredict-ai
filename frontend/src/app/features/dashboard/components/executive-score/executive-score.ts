import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QualityIntelligence } from '../../../../core/models/quality-intelligence';
import { QisSonar } from '../../../../core/models/qis-sonar.model';

@Component({
  selector: 'app-executive-score',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './executive-score.html',
  styleUrl: './executive-score.css'
})
export class ExecutiveScore {

  @Input() score: QualityIntelligence | null = null;

  @Input() qisSonar: QisSonar | null = null;

  getFinalScore(): number {

    if (this.qisSonar?.finalQis != null) {
      return this.qisSonar.finalQis;
    }

    return this.score?.qualityIntelligenceScore ?? 0;
  }

  getStatus(): string {

    const score = this.getFinalScore();

    if (score >= 80) {
      return 'excellent';
    }

    if (score >= 60) {
      return 'acceptable';
    }

    return 'critical';
  }

  getDecision(): string {

    const score = this.getFinalScore();

    if (score >= 80) {
      return 'GO';
    }

    if (score >= 60) {
      return 'GO WITH RISK';
    }

    return 'NO GO';
  }
}