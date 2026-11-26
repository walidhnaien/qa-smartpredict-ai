import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController
} from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { AiOrchestratorService } from './ai-orchestrator.service';
import baseline from '../models/testing/quality-report.fixture.json';

describe('AiOrchestratorService - Quality Report', () => {

  let service: AiOrchestratorService;
  let http: HttpTestingController;

  const runId = baseline.run.id;
  const url = `http://localhost:8081/api/ai/orchestrator/run/${runId}/result`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AiOrchestratorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  it('HTTP-01 retrieves the requested report using GET', () => {
    const next = vi.fn();

    service.result(runId).subscribe(next);

    const req = http.expectOne(url);

    expect(req.request.method).toBe('GET');
    expect(req.request.body).toBeNull();

    req.flush(baseline);

    expect(next).toHaveBeenCalledWith(baseline);
  });

  it('HTTP-02 propagates HTTP 500', () => {
    const error = vi.fn();

    service.result(runId).subscribe({ error });

    http.expectOne(url).flush(null, {
      status: 500,
      statusText: 'Internal Server Error'
    });

    expect(error).toHaveBeenCalledTimes(1);
    expect(error.mock.calls[0][0].status).toBe(500);
  });

  it('HTTP-03 propagates HTTP 404', () => {
    const error = vi.fn();

    service.result(runId).subscribe({ error });

    http.expectOne(url).flush(null, {
      status: 404,
      statusText: 'Not Found'
    });

    expect(error).toHaveBeenCalledTimes(1);
    expect(error.mock.calls[0][0].status).toBe(404);
  });

  it('HTTP-04 times out after 30 seconds', () => {
    vi.useFakeTimers();

    const error = vi.fn();

    service.result(runId).subscribe({ error });

    const req = http.expectOne(url);

    vi.advanceTimersByTime(29999);

    expect(error).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);

    expect(error).toHaveBeenCalledTimes(1);
    expect(error.mock.calls[0][0].name).toBe('TimeoutError');
    expect(req.cancelled).toBe(true);
  });

});
