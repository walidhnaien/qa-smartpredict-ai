import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { QualityRule } from '../models/quality-rule';

@Injectable({
  providedIn: 'root'
})
export class QualityRuleService {

  private apiUrl = 'http://localhost:8081/api/quality-rules';

  constructor(private http: HttpClient) {}

  getRules(): Observable<QualityRule[]> {
    return this.http.get<QualityRule[]>(this.apiUrl);
  }

  updateWeight(id: string, weight: number): Observable<QualityRule> {
    return this.http.put<QualityRule>(
      `${this.apiUrl}/${id}/weight?weight=${weight}`,
      {}
    );
  }
}