import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { QualityIntelligenceService } from './quality-intelligence';

describe('QualityIntelligenceService', () => {
  let service: QualityIntelligenceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(QualityIntelligenceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
