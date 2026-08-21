import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExecutiveScore } from './executive-score';

describe('ExecutiveScore', () => {
  let component: ExecutiveScore;
  let fixture: ComponentFixture<ExecutiveScore>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutiveScore],
    }).compileComponents();

    fixture = TestBed.createComponent(ExecutiveScore);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
