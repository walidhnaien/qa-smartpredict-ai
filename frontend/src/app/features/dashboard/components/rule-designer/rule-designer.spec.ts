import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuleDesigner } from './rule-designer';

describe('RuleDesigner', () => {
  let component: RuleDesigner;
  let fixture: ComponentFixture<RuleDesigner>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuleDesigner],
    }).compileComponents();

    fixture = TestBed.createComponent(RuleDesigner);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
