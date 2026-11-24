-- SIGEP · Triage asistido por IA
-- La IA solo SUGIERE (nivel_sugerido_ia); el nivel_final lo confirma una
-- persona (criterio clinico humano prevalece, ver exclusiones del alcance).
CREATE TABLE evaluaciones_triage (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    albergue_id BIGINT REFERENCES albergues (id) ON DELETE SET NULL,
    evaluado_por BIGINT REFERENCES usuarios (id),
    evaluado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Signos vitales
    frecuencia_cardiaca SMALLINT CHECK (frecuencia_cardiaca BETWEEN 0 AND 300),
    frecuencia_respiratoria SMALLINT CHECK (frecuencia_respiratoria BETWEEN 0 AND 100),
    presion_sistolica SMALLINT CHECK (presion_sistolica BETWEEN 0 AND 300),
    presion_diastolica SMALLINT CHECK (presion_diastolica BETWEEN 0 AND 200),
    temperatura NUMERIC(4,1) CHECK (temperatura BETWEEN 25 AND 45),
    saturacion_oxigeno SMALLINT CHECK (saturacion_oxigeno BETWEEN 0 AND 100),
    conciencia nivel_conciencia NOT NULL DEFAULT 'alerta',

    -- Clinica
    sintomas TEXT[] NOT NULL DEFAULT '{}',
    sindrome sindrome NOT NULL DEFAULT 'otro',
    estado_clinico estado_clinico NOT NULL DEFAULT 'estable',
    requiere_traslado BOOLEAN NOT NULL DEFAULT FALSE,
    notas TEXT,

    -- Resultado del triage
    nivel_sugerido_ia nivel_triage,
    puntaje_ia NUMERIC(5,2),
    explicacion_ia JSONB,                        -- reglas/variables que pesaron
    version_modelo VARCHAR(40),
    nivel_final nivel_triage,                    -- confirmado por personal medico
    confirmado_por BIGINT REFERENCES usuarios (id),
    confirmado_en TIMESTAMPTZ
);
