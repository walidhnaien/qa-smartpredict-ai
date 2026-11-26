import { jsPDF } from 'jspdf';

export class PdfLayoutEngine {

  readonly document: jsPDF;

  readonly margin = 16;
  readonly pageWidth: number;
  readonly pageHeight: number;
  readonly contentWidth: number;

  private readonly contentTop = 28;
  private readonly footerSpace = 20;

  private y = this.contentTop;

  constructor() {

    this.document = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });

    this.pageWidth =
      this.document.internal.pageSize.getWidth();

    this.pageHeight =
      this.document.internal.pageSize.getHeight();

    this.contentWidth =
      this.pageWidth - 2 * this.margin;

    this.drawHeader();
  }

  get cursorY(): number {
    return this.y;
  }

  get contentBottom(): number {
    return this.pageHeight - this.footerSpace;
  }

  private drawHeader(): void {

    const doc = this.document;

    doc.setFillColor(23, 45, 73);
    doc.rect(0, 0, this.pageWidth, 17, 'F');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(255, 255, 255);

    doc.text('QA SMARTPREDICT AI', this.margin, 11);

    doc.setTextColor(30, 41, 59);
  }

  newPage(): void {

    this.document.addPage();
    this.y = this.contentTop;

    this.drawHeader();
  }

  ensureSpace(height: number): void {

    const capacity =
      this.contentBottom - this.contentTop;

    if (!Number.isFinite(height) ||
        height < 0 ||
        height > capacity) {

      throw new Error('Invalid PDF block height.');
    }

    if (this.y + height > this.contentBottom) {
      this.newPage();
    }
  }

  space(height = 4): void {

    this.ensureSpace(height);
    this.y += height;
  }

  section(title: string): void {

    const doc = this.document;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(15);

    const lines: string[] =
      doc.splitTextToSize(title, this.contentWidth);

    this.ensureSpace(lines.length * 7 + 10);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(15);
    doc.setTextColor(23, 45, 73);

    for (const line of lines) {
      doc.text(line, this.margin, this.y);
      this.y += 7;
    }

    doc.setDrawColor(203, 213, 225);

    doc.line(
      this.margin,
      this.y,
      this.pageWidth - this.margin,
      this.y
    );

    this.y += 6;
  }

  paragraph(
    value: string | number | null | undefined,
    options: {
      bold?: boolean;
      fontSize?: number;
    } = {}
  ): void {

    const doc = this.document;
    const fontSize = options.fontSize ?? 10;

    if (!Number.isFinite(fontSize) ||
        fontSize < 8 ||
        fontSize > 24) {

      throw new Error('Invalid PDF font size.');
    }

    const lineHeight = fontSize * 0.45 + 1;

    const text =
      value == null || String(value).trim() === ''
        ? 'N/A'
        : String(value);

    const applyStyle = () => {

      doc.setFont(
        'helvetica',
        options.bold ? 'bold' : 'normal'
      );

      doc.setFontSize(fontSize);
      doc.setTextColor(30, 41, 59);
    };

    applyStyle();

    const lines: string[] =
      doc.splitTextToSize(text, this.contentWidth);

    for (const line of lines) {

      this.ensureSpace(lineHeight);
      applyStyle();

      doc.text(line, this.margin, this.y);

      this.y += lineHeight;
    }

    this.space(2);
  }

  field(
    label: string,
    value: string | number | null | undefined
  ): void {

    this.ensureSpace(16);

    this.paragraph(label, {
      bold: true,
      fontSize: 10
    });

    this.paragraph(value);
  }

  finalize(): jsPDF {

    const doc = this.document;
    const totalPages = doc.getNumberOfPages();

    for (let page = 1; page <= totalPages; page++) {

      doc.setPage(page);

      const footerY = this.pageHeight - 14;

      doc.setDrawColor(203, 213, 225);

      doc.line(
        this.margin,
        footerY,
        this.pageWidth - this.margin,
        footerY
      );

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8);
      doc.setTextColor(100, 116, 139);

      doc.text(
        'QA SmartPredict AI - V4.0.5',
        this.margin,
        footerY + 5
      );

      doc.text(
        `Page ${page} / ${totalPages}`,
        this.pageWidth - this.margin,
        footerY + 5,
        { align: 'right' }
      );
    }

    doc.setPage(totalPages);

    return doc;
  }
}
