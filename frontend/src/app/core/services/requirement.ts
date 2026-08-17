import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Requirement {
  id: string;
  code: string;
  title: string;
  criticality: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class RequirementService {

  private apiUrl = 'http://localhost:8081/api/requirements';

  constructor(private http: HttpClient) {}

  getRequirements(): Observable<Requirement[]> {
    return this.http.get<Requirement[]>(this.apiUrl);
  }
}