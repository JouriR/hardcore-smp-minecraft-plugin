CREATE TABLE IF NOT EXISTS piggy_bank
(
    id          INTEGER PRIMARY KEY                            NOT NULL,
    player_uuid TEXT                                           NOT NULL,
    amount      INTEGER                                        NOT NULL,
    is_assist   INTEGER DEFAULT 0 CHECK (is_assist IN (0, 1))  NOT NULL,
    is_penalty  INTEGER DEFAULT 0 CHECK (is_penalty IN (0, 1)) NOT NULL,
    created_at  TEXT    DEFAULT (datetime('now'))              NOT NULL,

    FOREIGN KEY (player_uuid) REFERENCES players (uuid) ON DELETE CASCADE
);