import { Component, Input, OnChanges, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom, forkJoin, Subscription, timer, exhaustMap, catchError, EMPTY } from 'rxjs';
import { AiOrchestratorService } from '../../../../core/services/ai-orchestrator.service';
import { WorkflowRun, WorkflowStep, Preflight, STEP_NAMES, active } from '../../../../core/models/ai-orchestrator.model';
@Component({selector:'app-ai-orchestrator', standalone:true, imports:[CommonModule], templateUrl:'./ai-orchestrator.html', styleUrl:'./ai-orchestrator.css'})
export class AiOrchestrator implements OnChanges, OnDestroy {
 @Input() releaseId: string | null = null;
 private api = inject(AiOrchestratorService);
 private polling?: Subscription;
 private epoch = 0;
 private destroyed = false;
 readonly names = STEP_NAMES;
 run = signal<WorkflowRun|null>(null);
 steps = signal<WorkflowStep[]>([]);
 preflight = signal<Preflight|null>(null);
 busy = signal(false);
 error = signal('');
 otherActive = signal(false);
 ready = signal(false);
 isActive = active;
 get completed() { return this.steps().filter(s => s.status === 'SUCCESS').length; }
 step(name: string) { return this.steps().find(s => s.step_name === name); }
 ngOnChanges() { void this.reconnect(); }
 ngOnDestroy() { this.destroyed = true; ++this.epoch; this.polling?.unsubscribe(); }
 private valid(e: number) { return !this.destroyed && e === this.epoch; }
 private message(err: any) { return err?.status === 409 ? 'A workflow is already active. Reconnect to check its status.' : err?.status === 503 ? 'Resources unavailable. Check Ollama and required models.' : 'Connection failed. Reconnect to recover the workflow; do not start a duplicate.'; }
 async reconnect() {
  const e = ++this.epoch; this.polling?.unsubscribe(); this.run.set(null); this.steps.set([]); this.ready.set(false); this.error.set(''); this.otherActive.set(false); this.preflight.set(null);
  if (!this.releaseId) { this.busy.set(false); return; }
  this.busy.set(true);
  try {
   const runs = await firstValueFrom(this.api.runs()); if (!this.valid(e)) return;
   const running = runs.find(r => active(r));
   const selected = runs.find(r => r.release_id === this.releaseId && r.mode === 'REAL' && active(r)) ?? runs.find(r => r.release_id === this.releaseId && r.mode === 'REAL');
   this.otherActive.set(!!running && running.id !== selected?.id);
   if (selected) { this.run.set(selected); this.watch(selected.id,e); }
   const pf = await firstValueFrom(this.api.preflight()); if (!this.valid(e)) return;
   this.preflight.set(pf); this.ready.set(true);
  } catch(err) { if(this.valid(e)) this.error.set(this.message(err)); }
  finally { if(this.valid(e)) this.busy.set(false); }
 }
 async start() {
  if (!this.releaseId || this.busy() || !this.ready() || !this.preflight()?.readyToRun || active(this.run()) || this.otherActive()) return;
  const e = this.epoch; const release = this.releaseId; this.busy.set(true); this.ready.set(false); this.error.set('');
  try {
   const pf = await firstValueFrom(this.api.preflight()); if(!this.valid(e)) return; this.preflight.set(pf);
   if (!pf.readyToRun) { this.ready.set(true); return; }
   const response = await firstValueFrom(this.api.start(release)); if(!this.valid(e)) return;
   this.polling?.unsubscribe(); this.steps.set([]);
   this.run.set({id:response.runId, release_id:release, mode:'REAL',status:response.status,current_step:null,recovery_count:0,duration_ms:null,error_message:null});
   this.ready.set(true); this.watch(response.runId,e);
  } catch(err) { if(this.valid(e)) this.error.set(this.message(err)); }
  finally { if(this.valid(e)) this.busy.set(false); }
 }
 private watch(id: string,e: number) {
  this.polling?.unsubscribe();
  this.polling = timer(0,2000).pipe(exhaustMap(() => forkJoin({run:this.api.run(id),steps:this.api.steps(id)}).pipe(catchError(err => { if(this.valid(e)) this.error.set(this.message(err)); return EMPTY; })))).subscribe(({run,steps}) => {
   if(!this.valid(e)) return; this.run.set(run); this.steps.set(steps); this.error.set('');
   if(!active(run)) this.polling?.unsubscribe();
  });
 }
}
