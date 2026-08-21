import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import { QualitySnapshot }
  from '../models/quality-snapshot.model';

@Injectable({
  providedIn: 'root'
})
export class QualityHistoryService {

  private apiUrl =
    'http://localhost:8081/api/quality';

  constructor(
    private http: HttpClient
  ) {}

  getHistory():
    Observable<QualitySnapshot[]> {

    return this.http.get<QualitySnapshot[]>(
      `${this.apiUrl}/history`
    );
  }
}