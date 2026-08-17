import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  Requirement,
  RequirementService
} from '../../core/services/requirement';

@Component({
  selector: 'app-requirements',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './requirements.html',
  styleUrl: './requirements.css'
})
export class Requirements implements OnInit {

  requirements: Requirement[] = [];

  constructor(private requirementService: RequirementService) {
    console.log('CONSTRUCTOR Requirements');
  }

  ngOnInit(): void {
    console.log('NGONINIT Requirements');

    this.requirementService.getRequirements().subscribe({
      next: (data) => {
        console.log('DATA API:', data);
        this.requirements = data;
      },
      error: (err) => {
        console.error('ERREUR API:', err);
      }
    });
  }
}