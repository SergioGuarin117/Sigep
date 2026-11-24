-- SIGEP · Red asistencial: IPS/hospitales, camas, inventario y personal

CREATE TABLE ips (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL UNIQUE,
    nit VARCHAR(20),
    nivel_atencion SMALLINT CHECK (nivel_atencion BETWEEN 1 AND 4),
    direccion VARCHAR(255),
    ubicacion geography(Point, 4326),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_ips
    FOREIGN KEY (ips_id) REFERENCES ips (id) ON DELETE SET NULL;

-- Disponibilidad dinamica de camas por tipo.
CREATE TABLE capacidad_camas (
    id BIGSERIAL PRIMARY KEY,
    ips_id BIGINT NOT NULL REFERENCES ips (id) ON DELETE CASCADE,
    tipo tipo_cama NOT NULL,
    total INTEGER NOT NULL CHECK (total >= 0),
    ocupadas INTEGER NOT NULL DEFAULT 0 CHECK (ocupadas >= 0),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ips_id, tipo),
    CHECK (ocupadas <= total)
);
CREATE TRIGGER trg_camas_actualizado BEFORE UPDATE ON capacidad_camas
    FOR EACH ROW EXECUTE FUNCTION fn_actualizado_en();

-- Medicamentos criticos e insumos medicos.
CREATE TABLE inventario_ips (
    id BIGSERIAL PRIMARY KEY,
    ips_id BIGINT NOT NULL REFERENCES ips (id) ON DELETE CASCADE,
    categoria categoria_inventario NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    unidad VARCHAR(30) NOT NULL DEFAULT 'unidad',
    cantidad INTEGER NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ips_id, categoria, nombre)
);
CREATE TRIGGER trg_inventario_actualizado BEFORE UPDATE ON inventario_ips
    FOR EACH ROW EXECUTE FUNCTION fn_actualizado_en();

-- Personal de salud disponible por perfil.
CREATE TABLE personal_salud (
    id BIGSERIAL PRIMARY KEY,
    ips_id BIGINT NOT NULL REFERENCES ips (id) ON DELETE CASCADE,
    perfil VARCHAR(60) NOT NULL,                 -- medico general, enfermeria...
    total INTEGER NOT NULL CHECK (total >= 0),
    disponibles INTEGER NOT NULL DEFAULT 0 CHECK (disponibles >= 0),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ips_id, perfil),
    CHECK (disponibles <= total)
);
CREATE TRIGGER trg_personal_actualizado BEFORE UPDATE ON personal_salud
    FOR EACH ROW EXECUTE FUNCTION fn_actualizado_en();

-- Equipos de campo (pantalla "Equipos" de la app).
CREATE TABLE equipos_campo (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    funcion VARCHAR(80) NOT NULL,
    profesionales INTEGER NOT NULL DEFAULT 1 CHECK (profesionales > 0),
    albergue_id BIGINT REFERENCES albergues (id) ON DELETE SET NULL,  -- NULL = base municipal
    estado estado_equipo NOT NULL DEFAULT 'disponible'
);
