import { describe, it, expect, vi } from 'vitest';
import { PdfLayoutEngine } from './pdf-layout-engine';

describe('PdfLayoutEngine', () => {

  it('LAYOUT-01 creates an A4 document', () => {

    const engine = new PdfLayoutEngine();

    expect(engine.pageWidth).toBeCloseTo(210, 0);
    expect(engine.pageHeight).toBeCloseTo(297, 0);

    expect(
      engine.document.getNumberOfPages()
    ).toBe(1);
  });

  it('LAYOUT-02 adds a section', () => {

    const engine = new PdfLayoutEngine();
    const initialY = engine.cursorY;

    engine.section('Executive Summary');

    expect(engine.cursorY).toBeGreaterThan(initialY);
  });

  it('LAYOUT-03 paginates long paragraphs', () => {

    const engine = new PdfLayoutEngine();

    engine.paragraph(
      'Semantic evidence and quality assessment. '
        .repeat(500)
    );

    expect(
      engine.document.getNumberOfPages()
    ).toBeGreaterThan(1);

    expect(engine.cursorY)
      .toBeLessThanOrEqual(engine.contentBottom);
  });

  it('LAYOUT-04 protects footer space', () => {

    const engine = new PdfLayoutEngine();

    const remaining =
      engine.contentBottom - engine.cursorY;

    engine.space(remaining - 1);
    engine.ensureSpace(10);

    expect(
      engine.document.getNumberOfPages()
    ).toBe(2);
  });

  it('LAYOUT-05 displays missing values as N/A', () => {

    const engine = new PdfLayoutEngine();
    const spy = vi.spyOn(engine.document, 'text');

    engine.paragraph(null);
    engine.paragraph(undefined);
    engine.paragraph('');

    const values = spy.mock.calls.map(
      call => call[0]
    );

    expect(
      values.filter(value => value === 'N/A')
    ).toHaveLength(3);
  });

  it('LAYOUT-06 preserves zero values', () => {

    const engine = new PdfLayoutEngine();
    const spy = vi.spyOn(engine.document, 'text');

    engine.paragraph(0);

    expect(
      spy.mock.calls.some(call => call[0] === '0')
    ).toBe(true);
  });

  it('LAYOUT-07 rejects oversized blocks', () => {

    const engine = new PdfLayoutEngine();

    expect(() => engine.ensureSpace(1000))
      .toThrow('Invalid PDF block height.');
  });

  it('LAYOUT-08 generates a valid PDF header', () => {

    const engine = new PdfLayoutEngine();

    engine.section('Quality Assessment');
    engine.paragraph('Quality Index Score: 68.89');

    const pdf = engine.finalize();
    const buffer = pdf.output('arraybuffer');

    const signature = new TextDecoder().decode(
      new Uint8Array(buffer).slice(0, 5)
    );

    expect(signature).toBe('%PDF-');
    expect(buffer.byteLength).toBeGreaterThan(1000);
  });
});
