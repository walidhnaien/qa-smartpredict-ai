import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';

import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    http.verify();
  });

  it('should create and initialize without starting AI analysis', async () => {
    fixture.detectChanges();

    const requests = http.match(() => true);

    expect(requests.length).toBeGreaterThan(0);

    for (const request of requests) {
      expect(request.request.method).toBe('GET');
      request.flush([]);
    }

    await fixture.whenStable();

    expect(component).toBeTruthy();
    expect(component.releases).toEqual([]);
    expect(component.selectedReleaseId).toBeNull();
    expect(component.isAiGenerating).toBe(false);
    expect(component.analyzingRelease).toBe(false);

    http.expectNone(req => req.method === 'POST');
  });
});
