CREATE TABLE IF NOT EXISTS players
(
    uuid       TEXT PRIMARY KEY                             NOT NULL,
    username   TEXT                                         NOT NULL,
    is_alive   INTEGER DEFAULT 1 CHECK (is_alive IN (0, 1)) NOT NULL,
    created_at TEXT    DEFAULT (datetime('now'))            NOT NULL,
    updated_at TEXT    DEFAULT (datetime('now'))            NOT NULL
);