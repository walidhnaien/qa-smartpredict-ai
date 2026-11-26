import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';

import { Requirements } from './requirements';
import { RequirementService } from '../../core/services/requirement';

describe('Requirements', () => {
  let component: Requirements;
  let fixture: ComponentFixture<Requirements>;

  const serviceMock = {
    getRequirements: vi.fn()
  };

  beforeEach(async () => {
    serviceMock.getRequirements.mockReset();
    serviceMock.getRequirements.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Requirements],
      providers: [
        {
          provide: RequirementService,
          useValue: serviceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Requirements);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should initialize without calling the real backend', () => {
    expect(component).toBeTruthy();
    expect(serviceMock.getRequirements).toHaveBeenCalledTimes(1);
    expect(component.requirements).toEqual([]);
  });
});
