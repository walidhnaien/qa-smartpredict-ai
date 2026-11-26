import { ViewChild } from '@angular/core';
import { DashboardQualitySnapshot, WhatIfExportSnapshot } from '../../core/models/quality-report-export.model';
import { AiOrchestrator } from './components/ai-orchestrator/ai-orchestrator';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
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
import { Release } from '../../core/models/release.model';
import { ReleaseService } from '../../core/services/release.service';
import { finalize, Subscription, take } from 'rxjs';
import { QualityReportSnapshotCollector } from '../../core/services/pdf/quality-report-snapshot-collector.service';
import { DashboardScreenshotService } from '../../core/services/pdf/dashboard-screenshot.service';
import { QualityPdfReportService } from '../../core/services/pdf/quality-pdf-report.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AiOrchestrator, CommonModule, FormsModule,ExecutiveScore,AiQualityAdvisor,ReleaseDecision,QualityBreakdown,SonarHealth,QualityKpiCard,Timeline,RuleDesigner,WhatIfSimulator],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit, OnDestroy {

  @ViewChild(AiOrchestrator)
  private aiOrchestrator?: AiOrchestrator;

  screenshotExporting = false;
  screenshotExportError = '';
  screenshotExportMessage = '';

  private screenshotExportGeneration = 0;

  pdfExporting = false;
  pdfExportError = '';
  pdfExportMessage = '';

  private pdfExportGeneration = 0;
  private pdfExportSubscription?: Subscription;

  @ViewChild(WhatIfSimulator)
  private whatIfSimulator?: WhatIfSimulator;

  get whatIfContextGeneration(): number {
    return this.releaseLoadGeneration;
  }

  get whatIfSourceReady(): boolean {

    return !this.destroyed &&
      !this.loadingRelease &&
      !!this.selectedReleaseId &&
      this.loadedReleaseId === this.selectedReleaseId &&
      !!this.score &&
      !!this.qisSonar &&
      this.qisSonar.sonarScore != null &&
      Number.isFinite(this.qisSonar.sonarScore);
  }

  captureWhatIfSnapshot(): WhatIfExportSnapshot | null {

    const dashboard = this.captureDashboardSnapshot();
    const simulator = this.whatIfSimulator;

    if (
      !dashboard ||
      !simulator ||
      !this.whatIfSourceReady
    ) {
      return null;
    }

    const snapshot = simulator.captureSnapshot(
      dashboard.releaseId,
      this.releaseLoadGeneration
    );

    if (!snapshot) {
      return null;
    }

    const expectedValues = {
      coverage: dashboard.score.coverageScore,
      defect: dashboard.score.defectScore,
      feedback: dashboard.score.feedbackScore,
      incident: dashboard.score.incidentScore,
      rca: dashboard.score.rcaScore,
      sonar: dashboard.sonar.sonarScore
    };

    const expectedWeights = {
      coverage: this.getNormalizedWeight('coverage'),
      defect: this.getNormalizedWeight('defect'),
      feedback: this.getNormalizedWeight('feedback'),
      incident: this.getNormalizedWeight('incident'),
      rca: this.getNormalizedWeight('RCA'),
      sonar: 20
    };

    const keys = [
      'coverage', 'defect', 'feedback',
      'incident', 'rca', 'sonar'
    ] as const;

    if (
      snapshot.currentQis !== dashboard.finalQis ||
      keys.some(key =>
        snapshot.currentValues[key] !== expectedValues[key]
      ) ||
      keys.some(key =>
        snapshot.weights[key] !== expectedWeights[key]
      )
    ) {
      return null;
    }

    return snapshot;
  }



  captureDashboardSnapshot(): DashboardQualitySnapshot | null {

    const releaseId = this.selectedReleaseId;
    const capturedAt = this.releaseMetricsCapturedAt;

    if (
      this.destroyed ||
      this.loadingRelease ||
      !releaseId ||
      this.loadedReleaseId !== releaseId ||
      !capturedAt ||
      !Number.isFinite(Date.parse(capturedAt)) ||
      !this.score ||
      !this.qisSonar
    ) {
      return null;
    }

    const finalQis = this.qisSonar.finalQis;

    if (
      !Number.isFinite(finalQis) ||
      finalQis < 0 ||
      finalQis > 100
    ) {
      return null;
    }

    return structuredClone({
      releaseId,
      capturedAt,
      score: this.score,
      sonar: this.qisSonar,
      finalQis
    });
  }



  private releaseLoadGeneration = 0;
  private destroyed = false;

  private loadedReleaseId: string | null = null;
  private releaseMetricsCapturedAt: string | null = null;

  private isCurrentReleaseLoad(
    releaseId: string,
    generation: number
  ): boolean {
    return !this.destroyed &&
      this.selectedReleaseId === releaseId &&
      this.releaseLoadGeneration === generation;
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.cancelPdfExport();
    ++this.releaseLoadGeneration;
    ++this.aiRecommendationRequestId;
    clearInterval(this.aiTimer);
  }

 aiIntelligence: AiIntelligenceResponse | null = null;
  qisSonar: QisSonar | null = null;
  score: QualityIntelligence | null = null;
  aiRecommendation: AiRecommendation | null = null;
  rules: QualityRule[] = [];
  ruleMessage = '';
  releases: Release[] = [];
selectedReleaseId: string | null = null;
loadingRelease = false;
analyzingRelease = false;
isAiGenerating = false;
aiElapsedSeconds = 0;
aiGenerationError = '';

private aiTimer?: ReturnType<typeof setInterval>;
private aiRecommendationRequestId = 0;
isAiRecommendationLoading = false;
aiRecommendationError = '';
analysisMessage = '';
analysisError = '';
timelineRefreshKey = 0;



  constructor(
    private service: QualityIntelligenceService,
	private aiService: AiRecommendationService,
	 private ruleService: QualityRuleService,
	 private sonarService: SonarService,
	 private aiIntelligenceService: AiIntelligenceService,
	 private releaseService: ReleaseService,
    private cdr: ChangeDetectorRef,
    private snapshotCollector: QualityReportSnapshotCollector,
    private pdfReportService: QualityPdfReportService,
    private screenshotService: DashboardScreenshotService
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

  /*
   * Règles de pondération
   */
  this.loadRules();

  /*
   * Chargement des Releases.
   * loadReleases() sélectionnera ensuite
   * automatiquement la Release de démo.
   */
  this.loadReleases();

  // AI QA Assistant sera généré après le chargement des métriques de la release.

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

  this.loadRules();

  if (this.selectedReleaseId) {
    this.loadSelectedRelease();
  }

}


loadSonarQis(): void {

  const projectId = 1;

  this.sonarService
    .getQisWithSonar(projectId)
    .subscribe({

      next: (data) => {

        console.log(
          'SONAR QIS DYNAMIQUE:',
          data
        );

        this.qisSonar = data;

        // AI execution is explicit via the orchestrator.

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


loadSelectedRelease(): void {

  this.cancelPdfExport();
  this.pdfExportError = '';
  this.pdfExportMessage = '';

  const generation = ++this.releaseLoadGeneration;
  const releaseId = this.selectedReleaseId;

  this.loadedReleaseId = null;
  this.releaseMetricsCapturedAt = null;

  this.score = null;
  this.qisSonar = null;
  this.aiIntelligence = null;
  this.aiRecommendation = null;
  this.aiRecommendationError = '';
  this.isAiRecommendationLoading = false;

  ++this.aiRecommendationRequestId;

  if (!releaseId || this.destroyed) {
    this.loadingRelease = false;
    return;
  }

  this.loadingRelease = true;

  this.service
    .getScoreByRelease(releaseId)
    .subscribe({

      next: data => {

        if (!this.isCurrentReleaseLoad(
          releaseId, generation
        )) return;

        this.score = data;

        this.loadSonarQisByRelease(
          releaseId, generation
        );

        this.cdr.detectChanges();
      },

      error: err => {

        if (!this.isCurrentReleaseLoad(
          releaseId, generation
        )) return;

        this.loadingRelease = false;

        console.error(
          'Erreur Release Quality:', err
        );

        this.cdr.detectChanges();
      }
    });
}

loadSonarQisByRelease(
  releaseId: string,
  generation: number
): void {

  if (!this.isCurrentReleaseLoad(
    releaseId, generation
  )) return;

  this.sonarService
    .getQisWithSonarByRelease(releaseId, 1)
    .subscribe({

      next: data => {

        if (!this.isCurrentReleaseLoad(
          releaseId, generation
        )) return;

        this.qisSonar = data;

        if (this.score && data) {
          this.loadedReleaseId = releaseId;
          this.releaseMetricsCapturedAt =
            new Date().toISOString();
        }

        this.loadingRelease = false;

        this.cdr.detectChanges();
      },

      error: err => {

        if (!this.isCurrentReleaseLoad(
          releaseId, generation
        )) return;

        this.loadingRelease = false;

        console.error(
          'Erreur Release Sonar QIS:', err
        );

        this.cdr.detectChanges();
      }
    });
}

onReleaseChange(): void {
  this.loadSelectedRelease();
}

loadReleases(): void {

  this.releaseService
    .getReleases()
    .subscribe({

      next: (data) => {

        this.releases = data;

        console.log(
          'RELEASES:',
          this.releases
        );

        /*
         * Pour la démo :
         * sélection automatique de 202603.0.0-BRM
         */
        const demoRelease =
          this.releases.find(
            r => r.version === '202603.0.0-BRM'
          );

        if (demoRelease) {

          this.selectedReleaseId =
            demoRelease.id;

          this.loadSelectedRelease();
        }

        this.cdr.detectChanges();
      },

      error: (err) => {

        console.error(
          'Erreur chargement Releases:',
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

  this.isAiGenerating = true;
  this.aiElapsedSeconds = 0;
  this.aiGenerationError = '';

  clearInterval(this.aiTimer);

  this.aiTimer = setInterval(() => {
    this.aiElapsedSeconds++;
    this.cdr.detectChanges();
  }, 1000);

  this.aiIntelligenceService
    .analyze(payload)
    .pipe(
      finalize(() => {
        clearInterval(this.aiTimer);
        this.isAiGenerating = false;
        this.cdr.detectChanges();
      })
    )
    .subscribe({
      next: data => {
        console.log('AI INTELLIGENCE:', data);
        this.aiIntelligence = data;
        this.cdr.detectChanges();
      },

      error: err => {
        console.error('Erreur AI Intelligence:', err);
        this.aiGenerationError =
          'AI analysis failed. Please try again.';
        this.cdr.detectChanges();
      }
    });
}




loadAiRecommendation(): void {
  if (!this.score || !this.qisSonar || !this.selectedReleaseId) {
    return;
  }

  const requestId = ++this.aiRecommendationRequestId;
  const metrics: Record<string, number> = {
    qis: this.getFinalScore(),
    coverageScore: this.score.coverageScore,
    defectScore: this.score.defectScore,
    feedbackScore: this.score.feedbackScore,
    incidentScore: this.score.incidentScore
  };

  this.aiRecommendation = null;
  this.aiRecommendationError = '';
  this.isAiRecommendationLoading = true;
  this.cdr.detectChanges();

  this.aiService.generateForRelease(metrics)
    .pipe(finalize(() => {
      if (requestId === this.aiRecommendationRequestId) {
        this.isAiRecommendationLoading = false;
        this.cdr.detectChanges();
      }
    }))
    .subscribe({
      next: data => {
        if (requestId !== this.aiRecommendationRequestId) return;
        this.aiRecommendation = data;
        this.analysisDate = new Date();
        this.cdr.detectChanges();
      },
      error: err => {
        if (requestId !== this.aiRecommendationRequestId) return;
        console.error('Erreur AI QA Assistant:', err);
        this.aiRecommendationError = 'AI analysis failed. Please try again.';
        this.cdr.detectChanges();
      }
    });
}
get canExportPdf(): boolean {

  if (
    this.destroyed ||
    this.pdfExporting ||
    this.screenshotExporting ||
    this.loadingRelease ||
    this.analyzingRelease ||
    !this.selectedReleaseId
  ) {
    return false;
  }

  const releaseExists = this.releases.some(
    release => release.id === this.selectedReleaseId
  );

  return releaseExists &&
    !!this.aiOrchestrator?.getExportableRunId(
      this.selectedReleaseId
    );
}

private cancelPdfExport(): void {

  ++this.screenshotExportGeneration;
  this.screenshotExportError = '';
  this.screenshotExportMessage = '';

  ++this.pdfExportGeneration;

  this.pdfExportSubscription?.unsubscribe();
  this.pdfExportSubscription = undefined;

  this.pdfExporting = false;
}

private isCurrentPdfExport(
  releaseId: string,
  runId: string,
  generation: number
): boolean {

  return !this.destroyed &&
    this.pdfExportGeneration === generation &&
    this.selectedReleaseId === releaseId &&
    this.aiOrchestrator?.getExportableRunId(
      releaseId
    ) === runId;
}

get canExportDashboardPdf(): boolean {
  return !this.destroyed &&
    !this.pdfExporting &&
    !this.screenshotExporting &&
    !this.loadingRelease &&
    !this.analyzingRelease &&
    !this.isAiGenerating &&
    !!this.selectedReleaseId &&
    this.loadedReleaseId === this.selectedReleaseId &&
    this.releases.some(r => r.id === this.selectedReleaseId) &&
    !!this.score &&
    !!this.qisSonar;
}

async exportDashboardPdf(): Promise<void> {

  if (!this.canExportDashboardPdf) return;

  const releaseId = this.selectedReleaseId;
  const release = this.releases.find(r => r.id === releaseId);
  const element = document.getElementById('quality-report');

  if (!release || !element) {
    this.screenshotExportError = 'Dashboard is unavailable.';
    return;
  }

  const generation = ++this.screenshotExportGeneration;

  this.screenshotExporting = true;
  this.screenshotExportError = '';
  this.screenshotExportMessage = '';

  const isCurrent = (): boolean =>
    !this.destroyed &&
    generation === this.screenshotExportGeneration &&
    this.selectedReleaseId === releaseId &&
    !this.loadingRelease &&
    !this.analyzingRelease &&
    element.isConnected;

  try {

    const pdf = await this.screenshotService.generate(element);

    if (!isCurrent()) return;

    const version = release.version.replace(
      /[^a-zA-Z0-9._-]/g, '_'
    );

    const date = new Date().toISOString().slice(0, 10);

    pdf.save(
      `QA-SmartPredict-Dashboard-${version}-${date}.pdf`
    );

    this.screenshotExportMessage =
      'Dashboard PDF generated successfully.';

  } catch (error) {

    if (isCurrent()) {
      this.screenshotExportError =
        'Dashboard PDF export failed. Please try again.';
    }

  } finally {

    this.screenshotExporting = false;

    if (!this.destroyed) {
      this.cdr.markForCheck();
    }
  }
}

exportPdf(): void {

  if (!this.canExportPdf) return;

  const releaseId = this.selectedReleaseId;

  const release = this.releases.find(
    item => item.id === releaseId
  );

  const runId =
    this.aiOrchestrator?.getExportableRunId(
      releaseId
    );

  if (!releaseId || !release || !runId) {
    return;
  }

  const generation = ++this.pdfExportGeneration;

  this.pdfExporting = true;
  this.pdfExportError = '';
  this.pdfExportMessage = '';

  const fail = (error: unknown): void => {

    if (
      this.destroyed ||
      this.pdfExportGeneration !== generation
    ) return;

    console.error('PDF export failed:', error);

    this.pdfExportError =
      'PDF export failed. Please try again.';
  };

  const finish = (): void => {

    if (
      this.destroyed ||
      this.pdfExportGeneration !== generation
    ) return;

    this.pdfExporting = false;
    this.cdr.markForCheck();
  };

  try {

    const currentDashboard =
      this.captureDashboardSnapshot();

    const simulation = currentDashboard
      ? this.captureWhatIfSnapshot()
      : null;

    this.pdfExportSubscription =
      this.snapshotCollector.collect({
        release,
        runId,
        currentDashboard,
        simulation
      }).pipe(
        take(1),
        finalize(finish)
      ).subscribe({

        next: result => {

          if (!this.isCurrentPdfExport(
            releaseId, runId, generation
          )) {
            return;
          }

          try {

            const pdf = this.pdfReportService.generate(
              result.data,
              result.context
            );

            if (!this.isCurrentPdfExport(
              releaseId, runId, generation
            )) {
              return;
            }

            const date = new Date()
              .toISOString()
              .slice(0, 10);

            const version = release.version
              .replace(/[^a-zA-Z0-9._-]/g, '_');

            pdf.save(
              `QA-SmartPredict-Quality-Report-${version}-${date}.pdf`
            );

            this.pdfExportMessage =
              result.warnings.length > 0
                ? 'PDF generated with limitations. See the report warnings.'
                : 'PDF generated successfully.';

          } catch (error) {
            fail(error);
          }
        },

        error: fail
      });

  } catch (error) {
    fail(error);
    finish();
  }
}


analyzeRelease(): void {

  if (!this.selectedReleaseId) {
    this.analysisError =
      'Please select a release first.';
    return;
  }

  const releaseId = this.selectedReleaseId;

  this.analyzingRelease = true;
  this.analysisMessage = '';
  this.analysisError = '';

  this.releaseService
    .analyzeRelease(releaseId)
    .subscribe({

      next: (result: QisSonar) => {

        console.log(
          'RELEASE ANALYSIS RESULT:',
          result
        );

        this.analysisMessage =
          `Analysis completed — Final QIS: ${result.finalQis}`;

        /*
         * Force le rafraîchissement de la Timeline
         */
        this.timelineRefreshKey++;

        /*
         * Recharge les KPI de la Release
         */
        this.loadSelectedRelease();

        this.analyzingRelease = false;

        this.cdr.detectChanges();
      },

      error: (err: unknown) => {

        console.error(
          'Release analysis failed:',
          err
        );

        this.analysisError =
          'Release analysis failed.';

        this.analyzingRelease = false;

        this.cdr.detectChanges();
      }

    });
}





























































 
}