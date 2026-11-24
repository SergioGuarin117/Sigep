-- SIGEP · Datos de EJEMPLO para desarrollo (coordenadas ficticias).
-- Borrar este archivo en produccion.

INSERT INTO albergues (nombre, direccion, ubicacion, capacidad_personas) VALUES
  ('La Rivera',  'Sector ribera norte',  ST_SetSRID(ST_MakePoint(-74.0820, 4.6100), 4326)::geography, 120),
  ('El Pinar',   'Colegio El Pinar',     ST_SetSRID(ST_MakePoint(-74.0700, 4.6200), 4326)::geography, 200),
  ('Las Flores', 'Coliseo Las Flores',   ST_SetSRID(ST_MakePoint(-74.0950, 4.6000), 4326)::geography, 150),
  ('El Puente',  'Salon comunal Puente', ST_SetSRID(ST_MakePoint(-74.0600, 4.5950), 4326)::geography,  80);

INSERT INTO ips (nombre, nivel_atencion, direccion, ubicacion) VALUES
  ('Hospital Central',     3, 'Av. Principal 10-20', ST_SetSRID(ST_MakePoint(-74.0750, 4.6050), 4326)::geography),
  ('Centro de Salud Norte', 1, 'Calle 50 # 8-15',     ST_SetSRID(ST_MakePoint(-74.0650, 4.6250), 4326)::geography);

INSERT INTO capacidad_camas (ips_id, tipo, total, ocupadas)
SELECT i.id, t.tipo::tipo_cama, t.total, t.ocupadas
FROM ips i
JOIN (VALUES ('Hospital Central','general',80,62), ('Hospital Central','uci',10,9),
             ('Hospital Central','urgencias',20,14),
             ('Centro de Salud Norte','general',15,5), ('Centro de Salud Norte','urgencias',6,2))
     AS t(ips, tipo, total, ocupadas) ON t.ips = i.nombre;

INSERT INTO inventario_ips (ips_id, categoria, nombre, unidad, cantidad, stock_minimo)
SELECT i.id, t.cat::categoria_inventario, t.nombre, t.unidad, t.cant, t.minimo
FROM ips i
JOIN (VALUES ('Hospital Central','medicamento','Suero oral','sobre',400,200),
             ('Hospital Central','medicamento','Acetaminofen 500 mg','tableta',1200,500),
             ('Hospital Central','insumo','Guantes de nitrilo','caja',60,40),
             ('Centro de Salud Norte','medicamento','Suero oral','sobre',90,150),
             ('Centro de Salud Norte','insumo','Guantes de nitrilo','caja',12,20))
     AS t(ips, cat, nombre, unidad, cant, minimo) ON t.ips = i.nombre;

INSERT INTO personal_salud (ips_id, perfil, total, disponibles)
SELECT i.id, t.perfil, t.total, t.disp
FROM ips i
JOIN (VALUES ('Hospital Central','medico general',12,3), ('Hospital Central','enfermeria',30,8),
             ('Centro de Salud Norte','medico general',3,2), ('Centro de Salud Norte','enfermeria',6,4))
     AS t(ips, perfil, total, disp) ON t.ips = i.nombre;

INSERT INTO equipos_campo (codigo, funcion, profesionales, albergue_id, estado) VALUES
  ('EQ-01', 'Brigada de censo',   3, (SELECT id FROM albergues WHERE nombre = 'La Rivera'), 'en_campo'),
  ('EQ-02', 'Atencion medica',    2, (SELECT id FROM albergues WHERE nombre = 'El Pinar'),  'en_campo'),
  ('EQ-03', 'Saneamiento basico', 2, NULL, 'disponible');

-- Usuarios de PRUEBA, uno por rol. Contrasena para todos: Sigep2026*
-- (hash bcrypt generado con pgcrypto; compatible con bcryptjs del backend).
INSERT INTO usuarios (nombre, apellido, correo, contrasena_hash, rol, albergue_id, ips_id)
SELECT t.nombre, t.apellido, t.correo, crypt('Sigep2026*', gen_salt('bf', 10)), t.rol,
       (SELECT id FROM albergues WHERE nombre = t.albergue),
       (SELECT id FROM ips WHERE nombre = t.ips)
FROM (VALUES
  ('Brigadista',   'Prueba', 'brigadista@sigep.test',   'brigadista',            'La Rivera', NULL),
  ('Medico',       'Prueba', 'medico@sigep.test',       'medico_triage',         'El Pinar',  NULL),
  ('AdminAlbergue','Prueba', 'albergue@sigep.test',     'admin_albergue',        'El Pinar',  NULL),
  ('DirectorIPS',  'Prueba', 'ips@sigep.test',          'director_ips',          NULL,        'Hospital Central'),
  ('Coordinador',  'Prueba', 'coordinador@sigep.test',  'coordinador_municipal', NULL,        NULL)
) AS t(nombre, apellido, correo, rol, albergue, ips);
