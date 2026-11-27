// MODELO: acceso a datos de la tabla "albergues". No conoce HTTP.
const pool = require("../config/db");

// personas_alojadas = pacientes con caso abierto asignados al albergue.
const SELECT_ALBERGUE = `
  SELECT a.id, a.nombre, a.direccion,
         ST_Y(a.ubicacion::geometry) AS latitud,
         ST_X(a.ubicacion::geometry) AS longitud,
         a.capacidad_personas, a.activo, a.creado_en,
         (SELECT COUNT(*) FROM pacientes p
           WHERE p.albergue_id = a.id AND NOT p.cerrado) AS personas_alojadas
  FROM albergues a`;

const PUNTO = (lon, lat) => `ST_SetSRID(ST_MakePoint($${lon}, $${lat}), 4326)::geography`;

function escaparLike(texto) {
  return texto.replace(/[\\%_]/g, "\\$&");
}

async function listar({ activo, texto }) {
  const condiciones = [];
  const valores = [];
  if (activo !== undefined) {
    valores.push(activo);
    condiciones.push(`a.activo = $${valores.length}`);
  }
  if (texto) {
    valores.push(`%${escaparLike(texto)}%`);
    condiciones.push(`(a.nombre ILIKE $${valores.length} OR a.direccion ILIKE $${valores.length})`);
  }
  const where = condiciones.length ? `WHERE ${condiciones.join(" AND ")}` : "";
  const resultado = await pool.query(`${SELECT_ALBERGUE} ${where} ORDER BY a.nombre`, valores);
  return resultado.rows;
}

async function buscarPorId(id) {
  const resultado = await pool.query(`${SELECT_ALBERGUE} WHERE a.id = $1`, [id]);
  return resultado.rows[0] || null;
}

async function crear({ nombre, direccion, latitud, longitud, capacidad_personas }) {
  const tieneUbicacion = latitud != null && longitud != null;
  const resultado = await pool.query(
    `INSERT INTO albergues (nombre, direccion, capacidad_personas, ubicacion)
     VALUES ($1, $2, $3, ${tieneUbicacion ? PUNTO(4, 5) : "NULL"})
     RETURNING id`,
    tieneUbicacion
      ? [nombre, direccion ?? null, capacidad_personas ?? 0, longitud, latitud]
      : [nombre, direccion ?? null, capacidad_personas ?? 0]
  );
  return buscarPorId(resultado.rows[0].id);
}

// cambios: subconjunto de nombre, direccion, capacidad_personas, activo y latitud+longitud.
async function actualizar(id, cambios) {
  const asignaciones = [];
  const valores = [];
  for (const campo of ["nombre", "direccion", "capacidad_personas", "activo"]) {
    if (campo in cambios) {
      valores.push(cambios[campo]);
      asignaciones.push(`${campo} = $${valores.length}`);
    }
  }
  if ("latitud" in cambios) {
    if (cambios.latitud === null) {
      asignaciones.push("ubicacion = NULL");
    } else {
      valores.push(cambios.longitud, cambios.latitud);
      asignaciones.push(`ubicacion = ${PUNTO(valores.length - 1, valores.length)}`);
    }
  }
  valores.push(id);
  const resultado = await pool.query(
    `UPDATE albergues SET ${asignaciones.join(", ")} WHERE id = $${valores.length} RETURNING id`,
    valores
  );
  return resultado.rows[0] ? buscarPorId(id) : null;
}

module.exports = { listar, buscarPorId, crear, actualizar };
