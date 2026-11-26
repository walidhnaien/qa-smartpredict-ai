import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';

import { Timeline } from './timeline';
import { QualityHistoryService }
  from '../../../../core/services/quality-history.service';

describe('Timeline', () => {
  let fixture: ComponentFixture<Timeline>;
  let component: Timeline;

  const historyMock = {
    getHistoryByRelease: vi.fn()
  };

  beforeEach(async () => {
    historyMock.getHistoryByRelease.mockReset();
    historyMock.getHistoryByRelease.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Timeline],
      providers: [{
        provide: QualityHistoryService,
        useValue: historyMock
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(Timeline);
    component = fixture.componentInstance;
  });

  function selectRelease(id = 'test-release-001') {
    fixture.componentRef.setInput('releaseId', id);
    fixture.detectChanges();
  }

  it('TL-01 loads history for the selected release', () => {
    selectRelease();

    expect(historyMock.getHistoryByRelease)
      .toHaveBeenCalledExactlyOnceWith('test-release-001');

    expect(component.history).toEqual([]);
    expect(component.loading).toBe(false);
    expect(component.error).toBe('');
  });

  it('TL-02 reloads history after release change', () => {
    selectRelease();
    selectRelease('test-release-002');

    expect(historyMock.getHistoryByRelease)
      .toHaveBeenCalledTimes(2);

    expect(historyMock.getHistoryByRelease)
      .toHaveBeenLastCalledWith('test-release-002');
  });

  it('TL-03 reloads history after refreshKey change', () => {
    selectRelease();

    fixture.componentRef.setInput('refreshKey', 1);
    fixture.detectChanges();

    expect(historyMock.getHistoryByRelease)
      .toHaveBeenCalledTimes(2);
  });

  it('TL-04 handles history API errors', () => {
    const errorSpy = vi.spyOn(console, 'error')
      .mockImplementation(() => {});

    try {
      historyMock.getHistoryByRelease.mockReturnValue(
        throwError(() => new Error('Simulated API failure'))
      );

      selectRelease();

      expect(component.error)
        .toBe('Unable to load quality history');

      expect(component.loading).toBe(false);
      expect(component.history).toEqual([]);
      expect(errorSpy).toHaveBeenCalled();
    } finally {
      errorSpy.mockRestore();
    }
  });

  it('TL-05 does not request history without a release', () => {
    const warnSpy = vi.spyOn(console, 'warn')
      .mockImplementation(() => {});

    try {
      fixture.detectChanges();

      expect(historyMock.getHistoryByRelease)
        .not.toHaveBeenCalled();

      expect(component.loading).toBe(false);
      expect(component.history).toEqual([]);

      expect(warnSpy).toHaveBeenCalledWith(
        'TIMELINE: no releaseId received'
      );
    } finally {
      warnSpy.mockRestore();
    }
  });
});
