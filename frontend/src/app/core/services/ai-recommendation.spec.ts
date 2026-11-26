import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AiRecommendationService } from './ai-recommendation';

describe('AiRecommendationService', () => {
  let service: AiRecommendationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AiRecommendationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
