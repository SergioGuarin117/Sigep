-- SIGEP · Albergues, factores de riesgo y censo epidemiologico

CREATE TABLE albergues (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    direccion VARCHAR(255),
    ubicacion geography(Point, 4326),          -- lon/lat (PostGIS)
    capacidad_personas INTEGER NOT NULL DEFAULT 0 CHECK (capacidad_personas >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_albergue
    FOREIGN KEY (albergue_id) REFERENCES albergues (id) ON DELETE SET NULL;

-- Factores de riesgo sanitario (agua, hacinamiento, desechos).
CREATE TABLE factores_riesgo_albergue (
    id BIGSERIAL PRIMARY KEY,
    albergue_id BIGINT NOT NULL REFERENCES albergues (id) ON DELETE CASCADE,
    calidad_agua nivel_riesgo NOT NULL,
    hacinamiento nivel_riesgo NOT NULL,
    manejo_desechos nivel_riesgo NOT NULL,
    personas_alojadas INTEGER CHECK (personas_alojadas >= 0),
    observaciones TEXT,
    registrado_por BIGINT REFERENCES usuarios (id),
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE grupos_familiares (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    albergue_id BIGINT REFERENCES albergues (id) ON DELETE SET NULL,
    zona_afectacion VARCHAR(160),               -- si no estan en albergue
    ubicacion geography(Point, 4326),
    numero_integrantes INTEGER NOT NULL DEFAULT 1 CHECK (numero_integrantes > 0),
    observaciones TEXT,
    registrado_por BIGINT REFERENCES usuarios (id),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pacientes (
    id BIGSERIAL PRIMARY KEY,
    numero SERIAL UNIQUE,                       -- P-001, C-001 en la app
    grupo_familiar_id BIGINT REFERENCES grupos_familiares (id) ON DELETE SET NULL,
    albergue_id BIGINT REFERENCES albergues (id) ON DELETE SET NULL,
    tipo_documento VARCHAR(20),                 -- CC, TI, RC, CE...
    numero_documento VARCHAR(30),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE,
    sexo VARCHAR(10) CHECK (sexo IN ('femenino', 'masculino', 'otro')),
    tipo_sangre VARCHAR(12),
    telefono VARCHAR(30),
    gestante BOOLEAN NOT NULL DEFAULT FALSE,
    condiciones_previas TEXT,
    observaciones TEXT,
    cerrado BOOLEAN NOT NULL DEFAULT FALSE,     -- caso cerrado
    datos_ocr JSONB,                            -- texto/confianza extraidos por OCR
    registrado_por BIGINT REFERENCES usuarios (id),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tipo_documento, numero_documento)
);
CREATE TRIGGER trg_pacientes_actualizado BEFORE UPDATE ON pacientes
    FOR EACH ROW EXECUTE FUNCTION fn_actualizado_en();

CREATE TABLE morbilidades_paciente (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    descripcion VARCHAR(200) NOT NULL,
    codigo_cie10 VARCHAR(10),
    desde DATE
);

CREATE TABLE medicamentos_cronicos (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    medicamento VARCHAR(160) NOT NULL,
    dosis VARCHAR(80),
    frecuencia VARCHAR(80),
    requiere_suministro BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vacunas_paciente (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    vacuna VARCHAR(120) NOT NULL,
    dosis_numero SMALLINT CHECK (dosis_numero > 0),
    fecha_aplicacion DATE
);
