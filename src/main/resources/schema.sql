CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS events (
    id            BIGSERIAL,
    time          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    app_id        TEXT         NOT NULL,
    app_version   TEXT         NOT NULL,
    os            TEXT         NOT NULL,
    arch          TEXT,
    event_type    TEXT         NOT NULL,
    feature       TEXT,
    duration_ms   BIGINT,
    error_type    TEXT,
    error_message TEXT,
    session_id    TEXT         NOT NULL,
    PRIMARY KEY (id, time)
);

SELECT create_hypertable('events', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_events_app_id ON events (app_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_events_event_type ON events (event_type, time DESC);
CREATE INDEX IF NOT EXISTS idx_events_feature ON events (feature, time DESC) WHERE feature IS NOT NULL;
