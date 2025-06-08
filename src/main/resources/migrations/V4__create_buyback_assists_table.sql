CREATE TABLE IF NOT EXISTS buyback_assists
(
    id                        INTEGER PRIMARY KEY            NOT NULL,
    giving_player_uuid        TEXT                           NOT NULL,
    receiving_player_uuid     TEXT                           NOT NULL,
    receiving_player_death_id INTEGER                        NOT NULL,
    amount                    REAL                           NOT NULL,
    created_at                TEXT DEFAULT (datetime('now')) NOT NULL,
    updated_at                TEXT DEFAULT (datetime('now')) NOT NULL,

    FOREIGN KEY (giving_player_uuid) REFERENCES players (uuid) ON DELETE CASCADE,
    FOREIGN KEY (receiving_player_uuid) REFERENCES players (uuid) ON DELETE CASCADE,
    FOREIGN KEY (receiving_player_death_id) REFERENCES deaths (id) ON DELETE CASCADE
);