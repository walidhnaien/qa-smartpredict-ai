import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AiIntelligenceResponse }
  from '../models/ai-intelligence.model';

@Injectable({
  providedIn: 'root'
})
export class AiIntelligenceService {

  private apiUrl =
    'http://localhost:8081/api/ai';

  constructor(
    private http: HttpClient
  ) {}

  analyze(
    payload: any
  ): Observable<AiIntelligenceResponse> {

    return this.http.post<AiIntelligenceResponse>(
      `${this.apiUrl}/analyze`,
      payload
    );
  }
}