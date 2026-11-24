-- SIGEP · Tipos enumerados (dominios cerrados del negocio)

-- Escala de Triage del documento guia.
CREATE TYPE nivel_triage AS ENUM ('verde', 'amarillo', 'rojo', 'negro');

-- Sindromes vigilados (la app los muestra como Febril, Diarreico agudo, etc.).
CREATE TYPE sindrome AS ENUM ('febril', 'gastrointestinal', 'respiratorio', 'trauma', 'otro');

CREATE TYPE estado_clinico AS ENUM ('estable', 'observacion', 'grave', 'critico');
CREATE TYPE nivel_conciencia AS ENUM ('alerta', 'voz', 'dolor', 'inconsciente');
CREATE TYPE nivel_riesgo AS ENUM ('bajo', 'medio', 'alto');

CREATE TYPE tipo_cama AS ENUM ('general', 'uci', 'urgencias');
CREATE TYPE categoria_inventario AS ENUM ('medicamento', 'insumo');
CREATE TYPE tipo_recurso AS ENUM ('cama', 'medicamento', 'insumo', 'personal');

CREATE TYPE nivel_alerta AS ENUM ('moderada', 'alta', 'critica');
CREATE TYPE estado_alerta AS ENUM ('evaluacion', 'seguimiento', 'intervencion', 'resuelta');
CREATE TYPE origen_alerta AS ENUM ('ia', 'manual');

CREATE TYPE estado_equipo AS ENUM ('en_campo', 'disponible');
CREATE TYPE estado_recomendacion AS ENUM ('sugerida', 'aprobada', 'rechazada', 'ejecutada');

-- Mantiene actualizado_en al modificar una fila.
CREATE OR REPLACE FUNCTION fn_actualizado_en() RETURNS trigger AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
