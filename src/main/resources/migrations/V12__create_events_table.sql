CREATE TABLE IF NOT EXISTS events
(
    id         INTEGER PRIMARY KEY                           NOT NULL,
    event_type INTEGER                                       NOT NULL,
    status     INTEGER DEFAULT 1 CHECK (status IN (1, 2, 3)) NOT NULL,
    created_at TEXT    DEFAULT (datetime('now'))             NOT NULL,
    updated_at TEXT    DEFAULT (datetime('now'))             NOT NULL,

    FOREIGN KEY (event_type) REFERENCES event_types (id) ON DELETE CASCADE
);