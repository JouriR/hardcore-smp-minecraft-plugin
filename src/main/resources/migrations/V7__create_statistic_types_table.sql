CREATE TABLE IF NOT EXISTS statistic_types
(
    id         INTEGER PRIMARY KEY            NOT NULL,
    name       TEXT                           NOT NULL UNIQUE,
    created_at TEXT DEFAULT (datetime('now')) NOT NULL
);