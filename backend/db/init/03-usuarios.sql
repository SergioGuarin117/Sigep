-- SIGEP · Usuarios y roles
-- Roles del documento guia:
--   brigadista, medico_triage, admin_albergue, director_ips, coordinador_municipal
-- El registro publico crea usuarios 'brigadista' (menor privilegio); el cambio
-- de rol lo hace un coordinador municipal.
CREATE TABLE IF NOT EXISTS public.usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    contrasena_hash TEXT NOT NULL,
    rol VARCHAR(30) NOT NULL DEFAULT 'brigadista'
        CHECK (rol IN ('brigadista', 'medico_triage', 'admin_albergue',
                       'director_ips', 'coordinador_municipal')),
    -- Asignacion segun el rol (se agregan las FK en 05 y 06).
    albergue_id BIGINT,
    ips_id BIGINT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
