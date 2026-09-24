-- ============================================================
-- Migration Sonar : ancien release_id BIGINT -> UUID
-- ============================================================

-- Les anciennes valeurs correspondent à l'ancien modèle
-- de Release basé sur Long et ne sont plus exploitables.
UPDATE sonar_metrics
SET release_id = NULL
WHERE release_id IS NOT NULL;

-- Conversion de la colonne vers UUID.
ALTER TABLE sonar_metrics
ALTER COLUMN release_id TYPE UUID
USING NULL::UUID;

-- Index pour la recherche de la dernière analyse par Release.
CREATE INDEX IF NOT EXISTS idx_sonar_metrics_release_id
ON sonar_metrics(release_id);