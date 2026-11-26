import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';

@Injectable({
  providedIn: 'root'
})
export class DashboardScreenshotService {

  async generate(element: HTMLElement): Promise<jsPDF> {

    if (!element.isConnected) {
      throw new Error('Dashboard is unavailable.');
    }

    const { default: html2canvas } =
      await import('html2canvas');

    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      onclone: (clonedDocument) => {

        const dashboard =
          clonedDocument.getElementById('quality-report');

        if (!dashboard) return;

        // Keep the visible dashboard without action buttons.
        dashboard.querySelectorAll('button').forEach(button => {
          button.style.display = 'none';
        });

        // Convert native disclosure elements into static
        // blocks for reliable PDF rendering.
        dashboard.querySelectorAll<HTMLDetailsElement>(
          'details.semantic-alert'
        ).forEach(details => {

          const block = clonedDocument.createElement('div');

          Array.from(details.attributes).forEach(attribute => {
            if (attribute.name !== 'open') {
              block.setAttribute(attribute.name, attribute.value);
            }
          });

          Object.assign(block.style, {
            display: 'block',
            position: 'relative',
            height: 'auto',
            maxHeight: 'none',
            overflow: 'visible',
            padding: '12px',
            marginBottom: '12px',
            border: '1px solid #dbe2ea',
            borderRadius: '8px',
            boxSizing: 'border-box'
          });

          const summary = details.querySelector('summary');

          if (summary) {
            const heading =
              clonedDocument.createElement('div');

            heading.textContent = summary.textContent;

            Object.assign(heading.style, {
              display: 'block',
              fontWeight: '700',
              padding: '10px',
              marginBottom: '12px',
              backgroundColor: '#f1f5f9',
              borderRadius: '4px',
              overflowWrap: 'anywhere'
            });

            block.appendChild(heading);
            summary.remove();
          }

          // Detailed semantic evidence remains available
          // in the structured Quality Report.
          const note = clonedDocument.createElement('p');

          note.textContent =
            'Full Before/After evidence is available in the Quality Report.';

          Object.assign(note.style, {
            fontSize: '12px',
            color: '#64748b',
            margin: '4px 0'
          });

          block.appendChild(note);

          details.replaceWith(block);
        });

        // Stabilize the layout during capture.
        dashboard.querySelectorAll<HTMLElement>('*')
          .forEach(item => {
            item.style.animation = 'none';
            item.style.transition = 'none';
          });
      }
    });

    if (canvas.width <= 0 || canvas.height <= 0) {
      throw new Error('Dashboard capture is empty.');
    }

    const pdf = new jsPDF('p', 'mm', 'a4');

    const pageWidth =
      pdf.internal.pageSize.getWidth();

    const pageHeight =
      pdf.internal.pageSize.getHeight();

    const margin = 10;
    const top = 18;
    const bottom = 15;

    const availableWidth =
      pageWidth - margin * 2;

    const availableHeight =
      pageHeight - top - bottom;

    const pixelsPerMm =
      canvas.width / availableWidth;

    const pagePixels = Math.max(
      1,
      Math.floor(availableHeight * pixelsPerMm)
    );

    const totalPages =
      Math.ceil(canvas.height / pagePixels);

    pdf.setProperties({
      title: 'QA SmartPredict AI - Dashboard Screenshot',
      subject: 'Visual dashboard snapshot',
      creator: 'QA SmartPredict AI'
    });

    for (let index = 0; index < totalPages; index++) {

      if (index > 0) {
        pdf.addPage();
      }

      const sourceY = index * pagePixels;

      const sliceHeight = Math.min(
        pagePixels,
        canvas.height - sourceY
      );

      const slice =
        document.createElement('canvas');

      slice.width = canvas.width;
      slice.height = sliceHeight;

      const context = slice.getContext('2d');

      if (!context) {
        throw new Error('Canvas context unavailable.');
      }

      context.drawImage(
        canvas,
        0,
        sourceY,
        canvas.width,
        sliceHeight,
        0,
        0,
        canvas.width,
        sliceHeight
      );

      pdf.setFontSize(10);
      pdf.setTextColor(31, 41, 55);

      pdf.text(
        'QA SmartPredict AI | Dashboard Screenshot',
        margin,
        10
      );

      pdf.addImage(
        slice.toDataURL('image/png'),
        'PNG',
        margin,
        top,
        availableWidth,
        sliceHeight / pixelsPerMm
      );

      pdf.setFontSize(8);
      pdf.setTextColor(100, 116, 139);

      pdf.text(
        'Visual snapshot - Not a validated audit report',
        margin,
        pageHeight - 7
      );

      pdf.text(
        `${index + 1} / ${totalPages}`,
        pageWidth - margin,
        pageHeight - 7,
        { align: 'right' }
      );

      slice.width = 0;
      slice.height = 0;
    }

    return pdf;
  }
}
