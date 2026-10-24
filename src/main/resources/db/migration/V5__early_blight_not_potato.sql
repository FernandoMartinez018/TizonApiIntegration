-- Tizón temprano y hojas que no son de papa (modelo de 4 salidas, contrato: SessionStats y Probabilities).

ALTER TABLE scan_sessions ADD COLUMN early_blight_frames INTEGER NOT NULL DEFAULT 0;
ALTER TABLE scan_sessions ADD COLUMN late_blight_frames INTEGER NOT NULL DEFAULT 0;
ALTER TABLE scan_sessions ADD COLUMN not_potato_frames INTEGER NOT NULL DEFAULT 0;

-- Hasta esta versión, afectado = tizón tardío.
UPDATE scan_sessions SET late_blight_frames = affected_frames;

ALTER TABLE scan_results ADD COLUMN prob_tizon_temprano DOUBLE PRECISION;
