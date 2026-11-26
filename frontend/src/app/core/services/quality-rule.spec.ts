import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { QualityRuleService } from './quality-rule';

describe('QualityRuleService', () => {
  let service: QualityRuleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(QualityRuleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
