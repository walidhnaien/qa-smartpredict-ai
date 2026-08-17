import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QualityRules } from './quality-rules';

describe('QualityRules', () => {
  let component: QualityRules;
  let fixture: ComponentFixture<QualityRules>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualityRules],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityRules);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
