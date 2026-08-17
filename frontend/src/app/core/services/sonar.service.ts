import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import { QisSonar } from '../models/qis-sonar.model';

@Injectable({
  providedIn: 'root'
})
export class SonarService {

  private apiUrl =
    'http://localhost:8081/api/sonar';

  constructor(
    private http: HttpClient
  ) {}

  getQisWithSonar(
    projectId: number,
    currentQis: number
  ): Observable<QisSonar> {

    return this.http.get<QisSonar>(
      `${this.apiUrl}/qis/${projectId}`,
      {
        params: {
          currentQis: currentQis
        }
      }
    );
  }

}