import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

import { QualityIntelligence } from '../../core/models/quality-intelligence';
import { QualityIntelligenceService } from '../../core/services/quality-intelligence';

import { AiRecommendation } from '../../core/models/ai-recommendation';
import { AiRecommendationService } from '../../core/services/ai-recommendation';

import { QualityRule } from '../../core/models/quality-rule';
import { QualityRuleService } from '../../core/services/quality-rule';
import { FormsModule } from '@angular/forms';

import { SonarService } from '../../core/services/sonar.service';

import { QisSonar } from '../../core/models/qis-sonar.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  qisSonar: QisSonar | null = null;
  score: QualityIntelligence | null = null;
  aiRecommendation: AiRecommendation | null = null;
  rules: QualityRule[] = [];
  ruleMessage = '';

  constructor(
    private service: QualityIntelligenceService,
	private aiService: AiRecommendationService,
	 private ruleService: QualityRuleService,
	 private sonarService: SonarService,
    private cdr: ChangeDetectorRef
  ) {}
  
  analysisDate = new Date();
  
  
  getStatus(): string {

  const finalScore = this.getFinalScore();

  if (finalScore >= 80) {
    return 'excellent';
  }

  if (finalScore >= 60) {
    return 'acceptable';
  }

  return 'critical';
}




  ngOnInit(): void {
    this.service.getScore().subscribe({
      next: (data) => {
        console.log('QIS DATA:', data);
        this.score = data;
		   this.loadSonarQis();
        this.cdr.detectChanges();
		this.loadRules();
      },
      error: (err) => {
        console.error('Erreur QIS:', err);
      }
    });
	
			this.aiService.getRecommendations().subscribe({
		  next: (data) => {
			console.log('AI DATA:', data);
			this.aiRecommendation = data;
			this.cdr.detectChanges();
		  },
		  error: (err) => {
			console.error('Erreur AI:', err);
		  }
		});
			
	
	
  }
  
  loadRules(): void {
  this.ruleService.getRules().subscribe({
    next: (data) => {
      this.rules = data;
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Erreur rules:', err);
    }
  });
}

getTotalWeight(): number {
  return this.rules
    .filter(r => r.enabled)
    .reduce((sum, r) => sum + Number(r.weight), 0);
}

saveRule(rule: QualityRule): void {
  this.ruleService.updateWeight(rule.id, rule.weight).subscribe({
    next: () => {
      this.ruleMessage = 'Weight updated successfully';
      this.refreshDashboard();
    },
    error: (err) => {
      console.error('Erreur update rule:', err);
    }
  });
}

refreshDashboard(): void {

  this.service.getScore().subscribe({

    next: (data) => {

      this.score = data;

      this.loadSonarQis();

      this.cdr.detectChanges();
    },

    error: (err) => {

      console.error(
        'Erreur refresh QIS:',
        err
      );
    }

  });


  this.aiService.getRecommendations().subscribe({

    next: (data) => {

      this.aiRecommendation = data;

      this.cdr.detectChanges();
    },

    error: (err) => {

      console.error(
        'Erreur refresh AI:',
        err
      );
    }

  });
}


loadSonarQis(): void {

  if (!this.score) {
    return;
  }

  const projectId = 1;

  const currentQis =
    this.score.qualityIntelligenceScore;

  this.sonarService
    .getQisWithSonar(
      projectId,
      currentQis
    )
    .subscribe({

      next: (data) => {

        console.log('SONAR QIS DATA:', data);

        this.qisSonar = data;

        this.cdr.detectChanges();
      },

      error: (err) => {

        console.error(
          'Erreur Sonar QIS:',
          err
        );
      }

    });
}
  
 getFinalScore(): number {

  if (this.qisSonar?.finalQis != null) {
    return this.qisSonar.finalQis;
  }

  return this.score?.qualityIntelligenceScore ?? 0;
} 
 

getRuleWeight(ruleName: string): number {

  const rule = this.rules.find(
    r => r.ruleName.toLowerCase()
      .includes(ruleName.toLowerCase())
  );

  return rule ? Number(rule.weight) : 0;
}

getContribution(
  score: number,
  ruleName: string
): number {

  const weight = this.getRuleWeight(ruleName);

  return score * weight / 100;
}

getSonarContribution(): number {

  if (!this.qisSonar) {
    return 0;
  }

  return this.qisSonar.sonarContribution;
}










































































 
}