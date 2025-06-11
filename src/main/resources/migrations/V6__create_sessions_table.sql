CREATE TABLE IF NOT EXISTS sessions
(
    id               INTEGER PRIMARY KEY               NOT NULL,
    player_uuid      TEXT                              NOT NULL,
    playtime_seconds INTEGER DEFAULT 0                 NOT NULL,
    created_at       TEXT    DEFAULT (datetime('now')) NOT NULL,

    FOREIGN KEY (player_uuid) REFERENCES players (uuid) ON DELETE CASCADE
);