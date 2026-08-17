import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef } from '@angular/core';

import { QualityRule } from '../../core/models/quality-rule';
import { QualityRuleService } from '../../core/services/quality-rule';

@Component({
  selector: 'app-quality-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quality-rules.html',
  styleUrl: './quality-rules.css'
})
export class QualityRules implements OnInit {

  rules: QualityRule[] = [];

  message = '';

  constructor(
    private service: QualityRuleService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRules();
  }

  loadRules(): void {
    this.service.getRules().subscribe(data => {
      this.rules = data;
      this.cdr.detectChanges();
    });
  }

  getTotalWeight(): number {
    return this.rules
      .filter(r => r.enabled)
      .reduce((sum, r) => sum + Number(r.weight), 0);
  }

  saveRule(rule: QualityRule): void {
    this.service.updateWeight(rule.id, rule.weight).subscribe(() => {
      this.message = 'Weight updated successfully';
      this.loadRules();
    });
  }
}