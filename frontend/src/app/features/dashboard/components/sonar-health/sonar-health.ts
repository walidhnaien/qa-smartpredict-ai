import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QisSonar } from '../../../../core/models/qis-sonar.model';

@Component({
  selector: 'app-sonar-health',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sonar-health.html',
  styleUrl: './sonar-health.css'
})
export class SonarHealth {

  @Input() sonar: QisSonar | null = null;

  getHealthStatus(): string {

  const sonarScore =
    this.sonar?.sonarScore ?? 0;

  const vulnerabilities =
    this.sonar?.vulnerabilities ?? 0;

  if (
    vulnerabilities === 0 &&
    sonarScore >= 80
  ) {
    return 'HEALTHY';
  }

  if (sonarScore >= 60) {
    return 'ATTENTION';
  }

  return 'CRITICAL';
}

  getStatusClass(): string {

    const status = this.getHealthStatus();

    if (status === 'HEALTHY') {
      return 'healthy';
    }

    if (status === 'ATTENTION') {
      return 'attention';
    }

    return 'critical';
  }

  getCoverageRating(): string {

    const coverage = this.sonar?.coverage ?? 0;

    if (coverage >= 80) {
      return 'A';
    }

    if (coverage >= 70) {
      return 'B';
    }

    if (coverage >= 60) {
      return 'C';
    }

    return 'D';
  }

  getDuplicationRating(): string {

    const duplication =
      this.sonar?.duplicatedLinesDensity ?? 0;

    if (duplication <= 3) {
      return 'A';
    }

    if (duplication <= 5) {
      return 'B';
    }

    if (duplication <= 10) {
      return 'C';
    }

    return 'D';
  }
}