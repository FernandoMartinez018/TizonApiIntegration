-- Recomendaciones por enfermedad (P-05, RF-08 CA3). El resumen de una sesión con hojas afectadas trae las de la
-- enfermedad que predominó entre esas hojas (en empate, tizón tardío). Son textos resumidos de guías técnicas, con
-- su fuente y sin nombres comerciales ni dosis. Las referencias completas están en
-- docs/requerimientos/fuentes-recomendaciones.md (rama sistema).
-- Reemplazan el texto genérico de V2. La columna severity sobra porque la severidad ya va en el resumen.

DELETE FROM recommendations;
DROP INDEX idx_recommendation_severity;
ALTER TABLE recommendations DROP COLUMN severity;
ALTER TABLE recommendations ADD COLUMN disease VARCHAR(20) NOT NULL;
CREATE INDEX idx_recommendation_disease ON recommendations (disease);

INSERT INTO recommendations (id, disease, text, source, sort_order) VALUES
    (1, 'TIZON_TARDIO',
     'Consulta a un ingeniero agrónomo para confirmar el diagnóstico e iniciar el control: la gota se maneja con un plan de fungicidas que alterna productos preventivos y sistémicos de distinto mecanismo de acción.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19; FHIA (2014), Manejo integrado del tizón tardío en papa, p. 14', 0),
    (2, 'TIZON_TARDIO',
     'En parcelas pequeñas, retira a mano las hojas con lesiones apenas las veas: así baja la cantidad de esporas que infectan el resto del cultivo.',
     'FHIA (2014), Manejo integrado del tizón tardío en papa, p. 7', 1),
    (3, 'TIZON_TARDIO',
     'Evita el riego por aspersión. Si es tu única opción, no riegues en la tarde, para que las hojas no pasen la noche mojadas.',
     'FHIA (2014), Manejo integrado del tizón tardío en papa, p. 7', 2),
    (4, 'TIZON_TARDIO',
     'Revisa el cultivo con frecuencia y sigue el pronóstico del tiempo: noches de 10 a 15 °C, días de 15 a 22 °C y hojas mojadas por 6 horas o más favorecen los ataques más fuertes, y en época de lluvias la gota completa su ciclo en 4 a 6 días.',
     'FHIA (2014), Manejo integrado del tizón tardío en papa, p. 5; ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19', 3),
    (5, 'TIZON_TARDIO',
     'Para la próxima siembra: semilla certificada, variedades tolerantes, siembra antes de las épocas de más lluvia, distancias que ventilen el follaje, drenajes, aporques altos, fertilización según el análisis de suelo sin exceso de nitrógeno y eliminación de las plantas de papa que nacen solas.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19; FHIA (2014), Manejo integrado del tizón tardío en papa, p. 6-7', 4),
    (6, 'TIZON_TEMPRANO',
     'Consulta a un ingeniero agrónomo para confirmar el diagnóstico y definir el control químico: el del tizón temprano es distinto al de la gota y da mejor resultado si se aplica temprano en la infección.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 20; Universidad de Wisconsin-Madison (2026), Potato Early Blight', 0),
    (7, 'TIZON_TEMPRANO',
     'Revisa la fertilización y el vigor del cultivo: el tizón temprano ataca sobre todo plantas desnutridas o debilitadas por otras plagas y enfermedades, y la falta de nitrógeno lo favorece.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19-20; Universidad de Wisconsin-Madison (2026), Potato Early Blight', 1),
    (8, 'TIZON_TEMPRANO',
     'Revisa primero las hojas de abajo y las más viejas: allí empieza la enfermedad, con manchas cafés de anillos concéntricos, y desde ahí sube por la planta.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 19; Universidad de Wisconsin-Madison (2026), Potato Early Blight', 2),
    (9, 'TIZON_TEMPRANO',
     'Destruye los residuos de cosecha y elimina las toyas y las malezas, donde el hongo sobrevive. Rota el lote con cultivos que no sean de la familia de la papa (solanáceas) durante 3 a 4 años.',
     'ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 20; Universidad de Wisconsin-Madison (2026), Potato Early Blight', 3),
    (10, 'TIZON_TEMPRANO',
     'Reduce la humedad del follaje: evita el exceso de riego, deja espacio entre plantas para que se ventilen y usa barreras rompevientos, porque el viento y la lluvia diseminan el hongo.',
     'Universidad de Wisconsin-Madison (2026), Potato Early Blight; ICA (2011), Manejo fitosanitario del cultivo de la papa, p. 20', 4);
