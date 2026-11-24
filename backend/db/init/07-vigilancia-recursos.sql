-- SIGEP · Vigilancia epidemiologica, recomendador de recursos y auditoria

CREATE TABLE alertas_epidemiologicas (
    id BIGSERIAL PRIMARY KEY,
    albergue_id BIGINT REFERENCES albergues (id) ON DELETE CASCADE,
    sindrome sindrome NOT NULL,
    nivel nivel_alerta NOT NULL,
    estado estado_alerta NOT NULL DEFAULT 'evaluacion',
    titulo VARCHAR(200) NOT NULL,
    casos_detectados INTEGER NOT NULL DEFAULT 0 CHECK (casos_detectados >= 0),
    ventana_inicio TIMESTAMPTZ,
    ventana_fin TIMESTAMPTZ,
    riesgo_ia NUMERIC(4,3) CHECK (riesgo_ia BETWEEN 0 AND 1),
    origen origen_alerta NOT NULL DEFAULT 'manual',
    detalle JSONB,
    creada_por BIGINT REFERENCES usuarios (id),
    creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_alertas_actualizado BEFORE UPDATE ON alertas_epidemiologicas
    FOR EACH ROW EXECUTE FUNCTION fn_actualizado_en();

-- Reasignacion sugerida de recursos (desde centros con superavit hacia saturados).
-- Origen y destino pueden ser una IPS o un albergue.
CREATE TABLE recomendaciones_recursos (
    id BIGSERIAL PRIMARY KEY,
    tipo tipo_recurso NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    origen_ips_id BIGINT REFERENCES ips (id),
    origen_albergue_id BIGINT REFERENCES albergues (id),
    destino_ips_id BIGINT REFERENCES ips (id),
    destino_albergue_id BIGINT REFERENCES albergues (id),
    justificacion TEXT,
    estado estado_recomendacion NOT NULL DEFAULT 'sugerida',
    generada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decidida_por BIGINT REFERENCES usuarios (id),
    decidida_en TIMESTAMPTZ,
    CHECK (num_nonnulls(origen_ips_id, origen_albergue_id) = 1),
    CHECK (num_nonnulls(destino_ips_id, destino_albergue_id) = 1)
);

-- Trazabilidad de acceso a datos sensibles de salud (Habeas Data).
CREATE TABLE auditoria_accesos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios (id) ON DELETE SET NULL,
    accion VARCHAR(40) NOT NULL,                 -- consulta, creacion, edicion, exportacion
    entidad VARCHAR(60) NOT NULL,                -- pacientes, evaluaciones_triage...
    entidad_id BIGINT,
    ip VARCHAR(45),
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
