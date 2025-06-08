CREATE TABLE IF NOT EXISTS deaths
(
    id          INTEGER PRIMARY KEY            NOT NULL,
    player_uuid TEXT                           NOT NULL,
    created_at  TEXT DEFAULT (datetime('now')) NOT NULL,

    FOREIGN KEY (player_uuid) REFERENCES players (uuid)
);