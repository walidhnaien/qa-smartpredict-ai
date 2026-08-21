import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AiIntelligenceResponse }
  from '../../../../core/models/ai-intelligence.model';

@Component({
  selector: 'app-ai-quality-advisor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-quality-advisor.html',
  styleUrl: './ai-quality-advisor.css'
})
export class AiQualityAdvisor {

  @Input()
  analysis: AiIntelligenceResponse | null = null;

  getRiskClass(): string {

    const risk =
      this.analysis?.riskLevel ?? '';

    if (risk === 'LOW') {
      return 'low';
    }

    if (risk === 'MEDIUM') {
      return 'medium';
    }

    return 'high';
  }

  getDecisionClass(): string {

    const decision =
      this.analysis?.releaseDecision ?? '';

    if (decision === 'GO') {
      return 'go';
    }

    if (decision === 'GO_WITH_RISK') {
      return 'risk';
    }

    return 'no-go';
  }
}