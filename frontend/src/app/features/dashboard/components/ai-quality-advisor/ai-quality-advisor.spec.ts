import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AiQualityAdvisor } from './ai-quality-advisor';

describe('AiQualityAdvisor', () => {
  let component: AiQualityAdvisor;
  let fixture: ComponentFixture<AiQualityAdvisor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiQualityAdvisor],
    }).compileComponents();

    fixture = TestBed.createComponent(AiQualityAdvisor);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
