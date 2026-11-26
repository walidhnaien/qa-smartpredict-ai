import { timeout } from 'rxjs';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { WorkflowRun, WorkflowStep, StartResponse, Preflight } from '../models/ai-orchestrator.model';
@Injectable({providedIn: 'root'})
export class AiOrchestratorService {
 private http = inject(HttpClient);
 private base = 'http://localhost:8081/api/ai/orchestrator';
 preflight() { return this.http.get<Preflight>(`${this.base}/preflight`); }
 runs() { return this.http.get<WorkflowRun[]>(`${this.base}/runs`); }
 start(releaseId: string) { return this.http.post<StartResponse>(`${this.base}/run`, {releaseId, mode:'REAL'}); }
 run(id: string) { return this.http.get<WorkflowRun>(`${this.base}/run/${id}`); }
 steps(id: string) { return this.http.get<WorkflowStep[]>(`${this.base}/run/${id}/steps`); }
 result(id: string) {
   return this.http.get<unknown>(
     `${this.base}/run/${id}/result`
   ).pipe(timeout(30000));
 }
}
