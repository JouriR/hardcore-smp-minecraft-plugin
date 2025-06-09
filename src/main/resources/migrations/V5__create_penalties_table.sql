CREATE TABLE IF NOT EXISTS penalties
(
    id          INTEGER PRIMARY KEY            NOT NULL,
    player_uuid TEXT                           NOT NULL,
    reason      TEXT                           NOT NULL,
    amount      REAL                           NOT NULL,
    created_at  TEXT DEFAULT (datetime('now')) NOT NULL,

    FOREIGN KEY (player_uuid) REFERENCES players (uuid) ON DELETE CASCADE
);