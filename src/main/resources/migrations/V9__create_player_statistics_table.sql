CREATE TABLE IF NOT EXISTS player_statistics
(
    id             INTEGER PRIMARY KEY            NOT NULL,
    player_uuid    TEXT                           NOT NULL,
    statistic_type INTEGER                        NOT NULL,
    value          INTEGER                        NOT NULL,
    created_at     TEXT DEFAULT (datetime('now')) NOT NULL,
    updated_at     TEXT DEFAULT (datetime('now')) NOT NULL,

    FOREIGN KEY (player_uuid) REFERENCES players (uuid) ON DELETE CASCADE,
    FOREIGN KEY (statistic_type) REFERENCES statistic_types (id) ON DELETE CASCADE,

    UNIQUE (player_uuid, statistic_type)
);