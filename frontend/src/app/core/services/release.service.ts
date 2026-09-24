import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Release } from '../models/release.model';
import { QisSonar } from '../models/qis-sonar.model';

@Injectable({
  providedIn: 'root'
})
export class ReleaseService {

  private readonly apiUrl =
    'http://localhost:8081/api/releases';

  constructor(
    private http: HttpClient
  ) {}

  getReleases(): Observable<Release[]> {

    return this.http.get<Release[]>(
      this.apiUrl
    );
  }


  analyzeRelease(
    releaseId: string
  ): Observable<QisSonar> {

    return this.http.post<QisSonar>(
      `${this.apiUrl}/${releaseId}/analyze`,
      {}
    );
  }

}