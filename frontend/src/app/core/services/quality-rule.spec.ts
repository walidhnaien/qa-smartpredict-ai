import { TestBed } from '@angular/core/testing';

import { QualityRule } from './quality-rule';

describe('QualityRule', () => {
  let service: QualityRule;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(QualityRule);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
