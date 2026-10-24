-- Alertas (F6-API-02, RF-11). Una por sesión: se crea al finalizar si affection_pct >= umbral
-- y analyzed_frames >= tizon.alert-min-frames.

CREATE TABLE alerts (
    id                  UUID          PRIMARY KEY,
    session_id          UUID          NOT NULL REFERENCES scan_sessions (id) ON DELETE CASCADE,
    username            VARCHAR(100)  NOT NULL,
    severity            VARCHAR(20)   NOT NULL,
    affection_pct       DOUBLE PRECISION NOT NULL,
    location_lat        DOUBLE PRECISION,
    location_lon        DOUBLE PRECISION,
    location_accuracy_m DOUBLE PRECISION,
    field_name          VARCHAR(120),
    status              VARCHAR(20)   NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_by     VARCHAR(100),
    acknowledged_at     TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX ux_alerts_session ON alerts (session_id);
CREATE INDEX idx_alerts_username_created ON alerts (username, created_at);
