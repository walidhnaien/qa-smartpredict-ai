import { TestBed } from '@angular/core/testing';

import { QualityIntelligence } from './quality-intelligence';

describe('QualityIntelligence', () => {
  let service: QualityIntelligence;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(QualityIntelligence);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
