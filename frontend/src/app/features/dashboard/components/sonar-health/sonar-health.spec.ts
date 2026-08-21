import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SonarHealth } from './sonar-health';

describe('SonarHealth', () => {
  let component: SonarHealth;
  let fixture: ComponentFixture<SonarHealth>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SonarHealth],
    }).compileComponents();

    fixture = TestBed.createComponent(SonarHealth);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
