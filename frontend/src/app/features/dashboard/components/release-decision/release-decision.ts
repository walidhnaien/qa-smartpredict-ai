import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QualityIntelligence } from '../../../../core/models/quality-intelligence';
import { QisSonar } from '../../../../core/models/qis-sonar.model';

@Component({
  selector: 'app-release-decision',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './release-decision.html',
  styleUrl: './release-decision.css'
})
export class ReleaseDecision {

  @Input() score: QualityIntelligence | null = null;
  @Input() qisSonar: QisSonar | null = null;

  getFinalScore(): number {

    if (this.qisSonar?.finalQis != null) {
      return this.qisSonar.finalQis;
    }

    return this.score?.qualityIntelligenceScore ?? 0;
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

  getDecisionClass(): string {

    const decision = this.getDecision();

    if (decision === 'GO') {
      return 'go';
    }

    if (decision === 'GO WITH RISK') {
      return 'risk';
    }

    return 'no-go';
  }

  getReadiness(): number {
    return this.getFinalScore();
  }

  getReasons(): string[] {

    const reasons: string[] = [];

    if (!this.score) {
      return reasons;
    }

    if (this.score.coverageScore < 70) {
      reasons.push(
        'Functional coverage is below the recommended level.'
      );
    }

    if (this.score.defectScore < 60) {
      reasons.push(
        'Defect quality remains a significant release risk.'
      );
    }

    if (this.score.feedbackScore >= 80) {
      reasons.push(
        'Feedback quality is strong.'
      );
    }

    if (this.score.incidentScore >= 80) {
      reasons.push(
        'Production stability is good.'
      );
    }

    if (
      this.qisSonar?.vulnerabilities != null &&
      this.qisSonar.vulnerabilities > 0
    ) {
      reasons.push(
        `${this.qisSonar.vulnerabilities} Sonar vulnerability detected.`
      );
    }

    if (
      this.qisSonar?.sonarScore != null &&
      this.qisSonar.sonarScore >= 70
    ) {
      reasons.push(
        'Technical quality measured by Sonar is acceptable.'
      );
    }

    return reasons;
  }

  getActions(): string[] {

    const actions: string[] = [];

    if (!this.score) {
      return actions;
    }

    if (this.score.coverageScore < 70) {
      actions.push(
        'Increase functional test coverage.'
      );
    }

    if (this.score.defectScore < 60) {
      actions.push(
        'Reduce unresolved high-risk defects.'
      );
    }

    if (
      this.qisSonar?.vulnerabilities != null &&
      this.qisSonar.vulnerabilities > 0
    ) {
      actions.push(
        'Fix Sonar vulnerabilities before release.'
      );
    }

    if (
      this.qisSonar?.codeSmells != null &&
      this.qisSonar.codeSmells > 30
    ) {
      actions.push(
        'Review technical debt and code smells.'
      );
    }

    if (actions.length === 0) {
      actions.push(
        'No blocking quality action identified.'
      );
    }

    return actions;
  }
}