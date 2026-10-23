-- Esquema base v2 (F2-API-02). Compatible con PostgreSQL 16 y con H2 en modo PostgreSQL (pruebas).
-- Los frames NO se almacenan: solo los resultados (RNF-SEG-12).

CREATE TABLE scan_sessions (
    id                   UUID          PRIMARY KEY,
    username             VARCHAR(100)  NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    total_frames         INTEGER       NOT NULL DEFAULT 0,
    analyzed_frames      INTEGER       NOT NULL DEFAULT 0,
    affected_frames      INTEGER       NOT NULL DEFAULT 0,
    no_vegetation_frames INTEGER       NOT NULL DEFAULT 0,
    avg_confidence       DOUBLE PRECISION,
    dominant_label       VARCHAR(20),
    severity             VARCHAR(20)   NOT NULL DEFAULT 'NONE',
    location_lat         DOUBLE PRECISION,
    location_lon         DOUBLE PRECISION,
    location_accuracy_m  DOUBLE PRECISION,
    field_name           VARCHAR(120),
    started_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at             TIMESTAMP WITH TIME ZONE,
    last_frame_at        TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_session_username ON scan_sessions (username);
CREATE INDEX idx_session_status   ON scan_sessions (status);

CREATE TABLE scan_results (
    id                UUID          PRIMARY KEY,
    session_id        UUID          NOT NULL REFERENCES scan_sessions (id) ON DELETE CASCADE,
    status            VARCHAR(20)   NOT NULL,
    label             VARCHAR(20),
    confidence        DOUBLE PRECISION,
    prob_sano         DOUBLE PRECISION,
    prob_tizon_tardio DOUBLE PRECISION,
    regions_json      TEXT          NOT NULL,
    frame_width       INTEGER       NOT NULL,
    frame_height      INTEGER       NOT NULL,
    model_version     VARCHAR(80)   NOT NULL,
    latency_ms        INTEGER       NOT NULL,
    inference_ms      INTEGER       NOT NULL,
    request_id        VARCHAR(80),
    analyzed_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_result_session_id  ON scan_results (session_id);
CREATE INDEX idx_result_analyzed_at ON scan_results (analyzed_at);
