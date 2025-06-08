CREATE TABLE IF NOT EXISTS deaths
(
    id         INTEGER PRIMARY KEY            NOT NULL,
    uuid       TEXT                           NOT NULL,
    created_at TEXT DEFAULT (datetime('now')) NOT NULL
);