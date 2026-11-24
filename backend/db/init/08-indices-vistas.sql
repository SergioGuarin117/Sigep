-- SIGEP · Indices (incluidos los espaciales) y vistas analiticas

-- Espaciales (GiST)
CREATE INDEX idx_albergues_ubicacion ON albergues USING GIST (ubicacion);
CREATE INDEX idx_ips_ubicacion ON ips USING GIST (ubicacion);
CREATE INDEX idx_grupos_ubicacion ON grupos_familiares USING GIST (ubicacion);

-- Consultas frecuentes
CREATE INDEX idx_pacientes_albergue ON pacientes (albergue_id);
CREATE INDEX idx_pacientes_grupo ON pacientes (grupo_familiar_id);
CREATE INDEX idx_triage_paciente ON evaluaciones_triage (paciente_id);
CREATE INDEX idx_triage_albergue_fecha ON evaluaciones_triage (albergue_id, evaluado_en);
CREATE INDEX idx_triage_sindrome_fecha ON evaluaciones_triage (sindrome, evaluado_en);
CREATE INDEX idx_alertas_albergue_estado ON alertas_epidemiologicas (albergue_id, estado);
CREATE INDEX idx_factores_albergue_fecha ON factores_riesgo_albergue (albergue_id, registrado_en);
CREATE INDEX idx_auditoria_usuario_fecha ON auditoria_accesos (usuario_id, ocurrido_en);

-- Casos por sindrome, albergue y dia: insumo del motor predictivo de brotes.
CREATE VIEW v_casos_sindrome_dia AS
SELECT albergue_id,
       sindrome,
       evaluado_en::date AS dia,
       COUNT(*) AS casos
FROM evaluaciones_triage
GROUP BY albergue_id, sindrome, evaluado_en::date;

-- Ocupacion de camas por IPS y tipo: insumo de la matriz demanda vs capacidad.
CREATE VIEW v_ocupacion_camas AS
SELECT i.id AS ips_id,
       i.nombre AS ips,
       c.tipo,
       c.total,
       c.ocupadas,
       c.total - c.ocupadas AS libres,
       CASE WHEN c.total = 0 THEN 0
            ELSE ROUND(c.ocupadas::numeric / c.total, 3) END AS ocupacion
FROM capacidad_camas c
JOIN ips i ON i.id = c.ips_id;
