CREATE TABLE IF NOT EXISTS countdowns
(
    id                   INTEGER PRIMARY KEY                                      NOT NULL,
    start_at             TEXT                                                     NOT NULL,
    finished             INTEGER DEFAULT 0 CHECK (finished IN (0, 1))             NOT NULL,
    created_at           TEXT    DEFAULT (datetime('now'))                        NOT NULL,
    updated_at           TEXT    DEFAULT (datetime('now'))                        NOT NULL
);