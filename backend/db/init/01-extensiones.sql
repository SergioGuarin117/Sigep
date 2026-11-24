-- SIGEP · Extensiones de PostgreSQL
-- PostGIS: tipos y funciones espaciales (albergues, IPS, alertas por zona).
CREATE EXTENSION IF NOT EXISTS postgis;

-- pgcrypto: crypt()/gen_salt() para sembrar usuarios de prueba con hash bcrypt.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
