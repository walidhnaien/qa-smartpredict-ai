export interface QisSonar {

  projectId: number;

  baseQis: number;

  sonarScore: number | null;

  sonarWeight: number;

  sonarContribution: number;

  baseQisContribution: number;

  finalQis: number;

  sonarQualityGate: string;

  bugs: number | null;

  vulnerabilities: number | null;

  codeSmells: number | null;

  coverage: number | null;

  duplicatedLinesDensity: number | null;
}