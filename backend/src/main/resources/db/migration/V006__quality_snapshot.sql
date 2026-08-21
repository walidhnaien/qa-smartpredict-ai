CREATE TABLE quality_snapshot (
    id UUID PRIMARY KEY,
    analysis_date TIMESTAMP NOT NULL,
    qis DOUBLE PRECISION NOT NULL,
    coverage_score DOUBLE PRECISION NOT NULL,
    defect_score DOUBLE PRECISION NOT NULL,
    feedback_score DOUBLE PRECISION NOT NULL,
    incident_score DOUBLE PRECISION NOT NULL,
    sonar_score DOUBLE PRECISION NOT NULL
);