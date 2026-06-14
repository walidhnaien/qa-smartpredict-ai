-- =====================================================
-- QA SmartPredict AI
-- V002__sample_data.sql
-- Demo Data
-- =====================================================

-- =====================================================
-- APPLICATION
-- =====================================================

INSERT INTO application (
    id,
    name,
    description,
    owner,
    status
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Top Day Extract',
    'Export des Top Day Cleared Trades',
    'QA Team',
    'ACTIVE'
);

-- =====================================================
-- RELEASE
-- =====================================================

INSERT INTO release (
    id,
    application_id,
    name,
    version,
    status,
    start_date
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Release 2026.1',
    '2026.1',
    'IN_PROGRESS',
    CURRENT_DATE
);

-- =====================================================
-- REQUIREMENTS
-- =====================================================

INSERT INTO requirement (
    id,
    application_id,
    code,
    title,
    description,
    criticality,
    status
)
VALUES
(
    '33333333-3333-3333-3333-333333333331',
    '11111111-1111-1111-1111-111111111111',
    'REQ-001',
    'Top Day Cleared Trades Export',
    'Exporter tous les Cleared Trades correspondant à la date d extraction',
    'BUSINESS_CRITICAL',
    'ACTIVE'
),

(
    '33333333-3333-3333-3333-333333333332',
    '11111111-1111-1111-1111-111111111111',
    'REQ-002',
    'GiveUp Trade Extraction',
    'Support des GiveUp trades selon le paramètre GiveUp Extraction',
    'BUSINESS_CRITICAL',
    'ACTIVE'
),

(
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'REQ-003',
    'Consolidation Status Filter',
    'Filtrage selon Consolidation Result Detailed ou All',
    'STANDARD',
    'ACTIVE'
);

-- =====================================================
-- USER STORIES
-- =====================================================

INSERT INTO user_story (
    id,
    release_id,
    jira_key,
    summary,
    description,
    status,
    priority,
    sprint
)
VALUES

(
    '44444444-4444-4444-4444-444444444441',
    '22222222-2222-2222-2222-222222222222',
    'TDE-101',
    'Implement Cleared Trades Export',
    'Export all eligible cleared trades',
    'DONE',
    'HIGH',
    'Sprint-1'
),

(
    '44444444-4444-4444-4444-444444444442',
    '22222222-2222-2222-2222-222222222222',
    'TDE-102',
    'Implement GiveUp Filter',
    'Support GiveUp Extraction options',
    'IN_PROGRESS',
    'HIGH',
    'Sprint-1'
);

-- =====================================================
-- REQUIREMENT ↔ USER STORY
-- =====================================================

INSERT INTO requirement_user_story (
    requirement_id,
    user_story_id
)
VALUES

(
    '33333333-3333-3333-3333-333333333331',
    '44444444-4444-4444-4444-444444444441'
),

(
    '33333333-3333-3333-3333-333333333332',
    '44444444-4444-4444-4444-444444444442'
);