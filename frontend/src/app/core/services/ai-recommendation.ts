import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AiRecommendation } from '../models/ai-recommendation';

@Injectable({
  providedIn: 'root'
})
export class AiRecommendationService {

  private apiUrl = 'http://localhost:8081/api/ai/generate';

  constructor(private http: HttpClient) {}

  getRecommendations(): Observable<AiRecommendation> {
    return this.http.get<AiRecommendation>(this.apiUrl);
  }
}