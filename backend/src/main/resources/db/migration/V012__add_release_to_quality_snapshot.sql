ALTER TABLE quality_snapshot
ADD COLUMN release_id UUID;

CREATE INDEX idx_quality_snapshot_release_id
ON quality_snapshot(release_id);