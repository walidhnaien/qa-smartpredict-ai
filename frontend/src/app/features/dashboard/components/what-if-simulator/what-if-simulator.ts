import {
  Component,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-what-if-simulator',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './what-if-simulator.html',
  styleUrl: './what-if-simulator.css'
})
export class WhatIfSimulator implements OnChanges {

  // Scores réels
  @Input() currentQis = 0;

  @Input() coverage = 0;
  @Input() defect = 0;
  @Input() feedback = 0;
  @Input() incident = 0;
  @Input() sonar = 0;

  // Poids QIS finaux
  @Input() coverageWeight = 0;
  @Input() defectWeight = 0;
  @Input() feedbackWeight = 0;
  @Input() incidentWeight = 0;
  @Input() sonarWeight = 20;

  // Valeurs de simulation
  simulatedCoverage = 0;
  simulatedDefect = 0;
  simulatedFeedback = 0;
  simulatedIncident = 0;
  simulatedSonar = 0;


  ngOnChanges(changes: SimpleChanges): void {

    this.reset();
  }


  reset(): void {

    this.simulatedCoverage = this.coverage;
    this.simulatedDefect = this.defect;
    this.simulatedFeedback = this.feedback;
    this.simulatedIncident = this.incident;
    this.simulatedSonar = this.sonar;
  }


  get simulatedQis(): number {

    const score =
      (this.simulatedCoverage * this.coverageWeight / 100)
      +
      (this.simulatedDefect * this.defectWeight / 100)
      +
      (this.simulatedFeedback * this.feedbackWeight / 100)
      +
      (this.simulatedIncident * this.incidentWeight / 100)
      +
      (this.simulatedSonar * this.sonarWeight / 100);

    return Math.round(score * 100) / 100;
  }


get difference(): number {

  const diff =
    this.simulatedQis - this.currentQis;

  if (Math.abs(diff) < 0.02) {
    return 0;
  }

  return Math.round(diff * 100) / 100;
}


  get currentDecision(): string {

    return this.getDecision(this.currentQis);
  }


  get simulatedDecision(): string {

    return this.getDecision(this.simulatedQis);
  }


  private getDecision(score: number): string {

    if (score >= 80) {
      return 'GO';
    }

    if (score >= 60) {
      return 'GO WITH RISK';
    }

    return 'NO GO';
  }
  





get targetQis(): number {
  return 80;
}

get gapToGo(): number {
  return Math.max(
    0,
    Math.round((this.targetQis - this.currentQis) * 100) / 100
  );
}

get recommendation(): {
  metric: string;
  currentValue: number;
  targetValue: number;
  improvement: number;
  projectedQis: number;
} | null {

  // Déjà GO
  if (this.currentQis >= this.targetQis) {
    return null;
  }

  const candidates = [
    {
      metric: 'Coverage',
      value: this.coverage,
      weight: this.coverageWeight
    },
    {
      metric: 'Defect Quality',
      value: this.defect,
      weight: this.defectWeight
    },
    {
      metric: 'Feedback',
      value: this.feedback,
      weight: this.feedbackWeight
    },
    {
      metric: 'Incident',
      value: this.incident,
      weight: this.incidentWeight
    },
    {
      metric: 'Sonar Quality',
      value: this.sonar,
      weight: this.sonarWeight
    }
  ];

  const possibleSolutions = candidates
    .filter(c => c.weight > 0)
    .map(c => {

      // Nombre de points KPI nécessaires
      // pour combler le gap QIS
      const requiredImprovement =
        this.gapToGo / (c.weight / 100);

      const targetValue =
        c.value + requiredImprovement;

      return {
        metric: c.metric,
        currentValue: c.value,
        targetValue: targetValue,
        improvement: requiredImprovement,
        projectedQis: this.targetQis
      };

    })

    // Impossible si le KPI devrait dépasser 100
    .filter(c => c.targetValue <= 100)

    // Plus petit effort nécessaire
    .sort(
      (a, b) =>
        a.improvement - b.improvement
    );

  if (possibleSolutions.length === 0) {
    return null;
  }

  const best = possibleSolutions[0];

  return {
    ...best,

    targetValue:
      Math.ceil(best.targetValue * 100) / 100,

    improvement:
      Math.round(best.improvement * 100) / 100
  };
}


applyRecommendation(): void {

  const rec = this.recommendation;

  if (!rec) {
    return;
  }

  switch (rec.metric) {

    case 'Coverage':
      this.simulatedCoverage =
        rec.targetValue;
      break;

    case 'Defect Quality':
      this.simulatedDefect =
        rec.targetValue;
      break;

    case 'Feedback':
      this.simulatedFeedback =
        rec.targetValue;
      break;

    case 'Incident':
      this.simulatedIncident =
        rec.targetValue;
      break;

    case 'Sonar Quality':
      this.simulatedSonar =
        rec.targetValue;
      break;
  }
}


getSimulationDecision(score: number): string {

  if (score >= 80) {
    return 'GO';
  }

  if (score >= 60) {
    return 'GO WITH RISK';
  }

  return 'NO GO';
}









  
  
  
  
  
}