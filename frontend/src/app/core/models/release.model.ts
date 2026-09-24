export interface Release {
  id: string;
  name: string;
  version: string;
  type: 'MAJOR' | 'EMERGENCY' | 'REGULATORY';
  status:
    | 'DRAFT'
    | 'IN_PROGRESS'
    | 'IN_ANALYSIS'
    | 'READY_FOR_DECISION'
    | 'RELEASED'
    | 'CANCELLED';

  startDate?: string;
  endDate?: string;
  createdAt?: string;
}