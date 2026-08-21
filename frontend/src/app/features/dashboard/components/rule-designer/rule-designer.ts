import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { QualityRule } from '../../../../core/models/quality-rule';

@Component({
  selector: 'app-rule-designer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rule-designer.html',
  styleUrl: './rule-designer.css'
})
export class RuleDesigner {

  @Input() rules: QualityRule[] = [];

  @Output() save =
    new EventEmitter<QualityRule>();

  @Output() refresh =
    new EventEmitter<void>();


  getTotalWeight(): number {

    return this.rules
      .filter(rule => rule.enabled)
      .reduce(
        (sum, rule) =>
          sum + Number(rule.weight),
        0
      );
  }


  getNormalizedWeight(rule: QualityRule): number {

    const total = this.getTotalWeight();

    if (!rule.enabled || total === 0) {
      return 0;
    }

    return (
      Number(rule.weight) / total
    ) * 100;
  }


  apply(rule: QualityRule): void {

    this.save.emit(rule);
  }
}