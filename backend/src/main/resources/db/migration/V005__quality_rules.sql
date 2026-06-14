CREATE TABLE quality_rule (
    id UUID PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    weight NUMERIC(5,2) NOT NULL,
    enabled BOOLEAN NOT NULL
);