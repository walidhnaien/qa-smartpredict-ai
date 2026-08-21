import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseDecision } from './release-decision';

describe('ReleaseDecision', () => {
  let component: ReleaseDecision;
  let fixture: ComponentFixture<ReleaseDecision>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseDecision],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseDecision);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
