import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QualityBreakdown } from './quality-breakdown';

describe('QualityBreakdown', () => {
  let component: QualityBreakdown;
  let fixture: ComponentFixture<QualityBreakdown>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualityBreakdown],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityBreakdown);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
