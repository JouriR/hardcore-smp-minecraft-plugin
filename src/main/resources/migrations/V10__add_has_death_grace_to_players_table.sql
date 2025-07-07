ALTER TABLE players
    ADD COLUMN has_death_grace INTEGER DEFAULT 0 CHECK (has_death_grace IN (0, 1)) NOT NULL;