CREATE TABLE release_requirement (
    release_id UUID NOT NULL,
    requirement_id UUID NOT NULL,

    CONSTRAINT pk_release_requirement
        PRIMARY KEY (release_id, requirement_id),

    CONSTRAINT fk_release_requirement_release
        FOREIGN KEY (release_id)
        REFERENCES release(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_release_requirement_requirement
        FOREIGN KEY (requirement_id)
        REFERENCES requirement(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_release_requirement_release
    ON release_requirement(release_id);

CREATE INDEX idx_release_requirement_requirement
    ON release_requirement(requirement_id);