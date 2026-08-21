import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExecutiveScore } from './components/executive-score/executive-score';
import { QualityIntelligence } from '../../core/models/quality-intelligence';
import { QualityIntelligenceService } from '../../core/services/quality-intelligence';
import { ReleaseDecision } from './components/release-decision/release-decision';
import { AiRecommendation } from '../../core/models/ai-recommendation';
import { AiRecommendationService } from '../../core/services/ai-recommendation';
import { QualityBreakdown }  from './components/quality-breakdown/quality-breakdown';
import { QualityRule } from '../../core/models/quality-rule';
import { QualityRuleService } from '../../core/services/quality-rule';
import { FormsModule } from '@angular/forms';
import { SonarHealth }  from './components/sonar-health/sonar-health';
import { SonarService } from '../../core/services/sonar.service';
import { QualityKpiCard }  from './components/quality-kpi-card/quality-kpi-card';
import { QisSonar } from '../../core/models/qis-sonar.model';
import { Timeline }   from './components/timeline/timeline';
import { RuleDesigner }  from './components/rule-designer/rule-designer';
import { WhatIfSimulator }   from './components/what-if-simulator/what-if-simulator';
import { AiIntelligenceService }  from '../../core/services/ai-intelligence.service';
import { AiIntelligenceResponse }  from '../../core/models/ai-intelligence.model';
import { AiQualityAdvisor }  from './components/ai-quality-advisor/ai-quality-advisor';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule,ExecutiveScore,AiQualityAdvisor,ReleaseDecision,QualityBreakdown,SonarHealth,QualityKpiCard,Timeline,RuleDesigner,WhatIfSimulator],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
 aiIntelligence: AiIntelligenceResponse | null = null;
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
	 private aiIntelligenceService: AiIntelligenceService,
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
		this.loadAiIntelligence();

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

  const normalizedWeight =
    this.getNormalizedWeight(ruleName);

  return score *
    normalizedWeight / 100;
}
getSonarContribution(): number {

  if (!this.qisSonar) {
    return 0;
  }

  return this.qisSonar.sonarContribution;
}

getTotalEnabledWeight(): number {

  return this.rules
    .filter(r => r.enabled)
    .reduce(
      (sum, r) => sum + Number(r.weight),
      0
    );
}

getNormalizedWeight(
  ruleName: string
): number {

  const sonarWeight = 20;

  const activeRules =
    this.rules.filter(r => r.enabled);

  const total =
    activeRules.reduce(
      (sum, r) => sum + Number(r.weight),
      0
    );

  if (total === 0) {
    return 0;
  }

  const rule =
    activeRules.find(
      r =>
        r.ruleName.toUpperCase()
        === ruleName.toUpperCase()
    );

  if (!rule) {
    return 0;
  }

  const functionalWeight =
    Number(rule.weight) / total;

  return functionalWeight *
    (100 - sonarWeight);
}


getCoverageStatus(): string {

  const score =
    this.score?.coverageScore ?? 0;

  if (score >= 80) {
    return 'STRONG';
  }

  if (score >= 70) {
    return 'ACCEPTABLE';
  }

  return 'NEEDS ATTENTION';
}

getDefectStatus(): string {

  const score =
    this.score?.defectScore ?? 0;

  if (score >= 80) {
    return 'STRONG';
  }

  if (score >= 60) {
    return 'ACCEPTABLE';
  }

  return 'HIGH RISK';
}


loadAiIntelligence(): void {

  if (!this.score || !this.qisSonar) {
    return;
  }

  const payload = {

    qis: this.getFinalScore(),

    coverageScore:
      this.score.coverageScore,

    defectScore:
      this.score.defectScore,

    feedbackScore:
      this.score.feedbackScore,

    incidentScore:
      this.score.incidentScore,

    sonarScore:
      this.qisSonar.sonarScore ?? 0,

    bugs:
      this.qisSonar.bugs,

    vulnerabilities:
      this.qisSonar.vulnerabilities,

    codeSmells:
      this.qisSonar.codeSmells,

    sonarCoverage:
      this.qisSonar.coverage,

    duplication:
      this.qisSonar.duplicatedLinesDensity,

    totalRequirements:
      this.score.totalRequirements,

    coveredRequirements:
      this.score.coveredRequirements,

    uncoveredRequirements:
      this.score.uncoveredRequirements,

    totalStories:
      this.score.totalStories,

    totalBugs:
      this.score.totalBugs,

    bugRatio:
      this.score.bugRatio
  };

  this.aiIntelligenceService
    .analyze(payload)
    .subscribe({

      next: data => {

        console.log(
          'AI INTELLIGENCE:',
          data
        );

        this.aiIntelligence = data;

        this.cdr.detectChanges();
      },

      error: err => {

        console.error(
          'Erreur AI Intelligence:',
          err
        );
      }

    });
}







































































 
}