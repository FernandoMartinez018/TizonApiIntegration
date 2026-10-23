-- Recomendaciones por severidad (F3-API-03, RF-08 CA3). No se inventan textos agronómicos:
-- mientras no haya una fuente validada (pendiente P-05) solo se usa el texto genérico.
-- Para NONE (sin afectación) no hay recomendación.

CREATE TABLE recommendations (
    id         INTEGER       PRIMARY KEY,
    severity   VARCHAR(20)   NOT NULL,
    text       VARCHAR(500)  NOT NULL,
    source     VARCHAR(300)  NOT NULL,
    sort_order INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_recommendation_severity ON recommendations (severity);

INSERT INTO recommendations (id, severity, text, source, sort_order) VALUES
    (1, 'LOW',      'Consulta a un técnico agrónomo', 'Texto genérico pendiente de validación agronómica (P-05)', 0),
    (2, 'WARNING',  'Consulta a un técnico agrónomo', 'Texto genérico pendiente de validación agronómica (P-05)', 0),
    (3, 'CRITICAL', 'Consulta a un técnico agrónomo', 'Texto genérico pendiente de validación agronómica (P-05)', 0);
