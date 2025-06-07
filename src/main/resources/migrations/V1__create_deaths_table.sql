CREATE TABLE IF NOT EXISTS deaths (
    uuid TEXT PRIMARY KEY NOT NULL,
    created_at TEXT DEFAULT (datetime('now')) NOT NULL
);