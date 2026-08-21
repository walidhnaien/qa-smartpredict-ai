import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-quality-kpi-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quality-kpi-card.html',
  styleUrl: './quality-kpi-card.css'
})
export class QualityKpiCard {

  @Input() title = '';
  @Input() score = 0;

  @Input() weight = 0;
  @Input() contribution = 0;

  @Input() status = '';

  @Input() details: {
    label: string;
    value: string | number;
  }[] = [];

  getStatusClass(): string {

    const status = this.status.toLowerCase();

    if (
      status.includes('excellent') ||
      status.includes('strong') ||
      status.includes('good')
    ) {
      return 'good';
    }

    if (
      status.includes('attention') ||
      status.includes('acceptable') ||
      status.includes('medium')
    ) {
      return 'attention';
    }

    return 'critical';
  }
}