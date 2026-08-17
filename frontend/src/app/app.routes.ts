import { Routes } from '@angular/router';
import { Requirements } from './features/requirements/requirements';
import { Dashboard } from './features/dashboard/dashboard';
import { QualityRules } from './features/quality-rules/quality-rules';

export const routes: Routes = [
  {
    path: '',
    component: Dashboard
  },
  {
    path: 'requirements',
    component: Requirements
  },
  {
    path: 'quality-rules',
    component: QualityRules
  },
];