import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QualityKpiCard } from './quality-kpi-card';

describe('QualityKpiCard', () => {
  let component: QualityKpiCard;
  let fixture: ComponentFixture<QualityKpiCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QualityKpiCard],
    }).compileComponents();

    fixture = TestBed.createComponent(QualityKpiCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
