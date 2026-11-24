ALTER TABLE ai_review_result
DROP CONSTRAINT chk_review_output_mode;

ALTER TABLE ai_review_result
ADD CONSTRAINT chk_review_output_mode
CHECK (
    output_mode IN (
        'TEXT',
        'JSON_MODE',
        'STRUCTURED_OUTPUT',
        'JSON_SCHEMA'
    )
);