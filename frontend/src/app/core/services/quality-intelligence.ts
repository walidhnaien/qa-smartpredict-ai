import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { QualityIntelligence }
from '../models/quality-intelligence';

@Injectable({
  providedIn: 'root'
})
export class QualityIntelligenceService {

  private apiUrl =
    'http://localhost:8081/api/quality-intelligence';

  constructor(private http: HttpClient) {}

  getScore():
    Observable<QualityIntelligence> {

    return this.http.get<QualityIntelligence>(
      this.apiUrl
    );
  }
}