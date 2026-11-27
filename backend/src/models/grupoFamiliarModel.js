// MODELO: acceso a datos de "grupos_familiares". No conoce HTTP.
const pool = require("../config/db");

const SELECT_GRUPO = `
  SELECT g.id, g.codigo, g.albergue_id, g.zona_afectacion,
         ST_Y(g.ubicacion::geometry) AS latitud,
         ST_X(g.ubicacion::geometry) AS longitud,
         g.numero_integrantes, g.observaciones, g.registrado_por, g.creado_en,
         (SELECT COUNT(*) FROM pacientes p WHERE p.grupo_familiar_id = g.id) AS integrantes_registrados
  FROM grupos_familiares g`;

function escaparLike(texto) {
  return texto.replace(/[\\%_]/g, "\\$&");
}

// El codigo (GF-0001) se arma con el id reservado de la secuencia, en una sola sentencia.
async function crear(datos, usuarioId) {
  const tieneUbicacion = datos.latitud != null && datos.longitud != null;
  const valores = [
    datos.albergue_id ?? null,
    datos.zona_afectacion ?? null,
    datos.numero_integrantes ?? 1,
    datos.observaciones ?? null,
    usuarioId,
  ];
  if (tieneUbicacion) valores.push(datos.longitud, datos.latitud);

  const resultado = await pool.query(
    `WITH n AS (SELECT nextval(pg_get_serial_sequence('grupos_familiares', 'id')) AS id)
     INSERT INTO grupos_familiares
       (id, codigo, albergue_id, zona_afectacion, numero_integrantes, observaciones, registrado_por, ubicacion)
     SELECT n.id, 'GF-' || LPAD(n.id::text, 4, '0'),
            $1::bigint, $2::varchar, $3::integer, $4::text, $5::bigint,
            ${tieneUbicacion ? "ST_SetSRID(ST_MakePoint($6::float8, $7::float8), 4326)::geography" : "NULL"}
     FROM n
     RETURNING id`,
    valores
  );
  return buscarPorId(resultado.rows[0].id);
}

async function listar({ albergue_id, texto, limite, desplazamiento }) {
  const condiciones = [];
  const valores = [];
  if (albergue_id !== undefined) {
    valores.push(albergue_id);
    condiciones.push(`g.albergue_id = $${valores.length}`);
  }
  if (texto) {
    valores.push(`%${escaparLike(texto)}%`);
    condiciones.push(`(g.codigo ILIKE $${valores.length} OR g.zona_afectacion ILIKE $${valores.length})`);
  }
  const where = condiciones.length ? `WHERE ${condiciones.join(" AND ")}` : "";
  valores.push(limite, desplazamiento);

  const resultado = await pool.query(
    `SELECT t.*, COUNT(*) OVER() AS total FROM (${SELECT_GRUPO} ${where}) t
     ORDER BY t.creado_en DESC, t.id DESC
     LIMIT $${valores.length - 1} OFFSET $${valores.length}`,
    valores
  );
  const total = resultado.rows.length ? Number(resultado.rows[0].total) : 0;
  return { total, grupos: resultado.rows.map(({ total: _t, ...grupo }) => grupo) };
}

async function buscarPorId(id) {
  const resultado = await pool.query(`${SELECT_GRUPO} WHERE g.id = $1`, [id]);
  return resultado.rows[0] || null;
}

async function integrantes(grupoId) {
  const resultado = await pool.query(
    `SELECT id, numero, nombres, apellidos, cerrado
     FROM pacientes WHERE grupo_familiar_id = $1 ORDER BY id`,
    [grupoId]
  );
  return resultado.rows;
}

async function actualizar(id, cambios) {
  const asignaciones = [];
  const valores = [];
  for (const campo of ["albergue_id", "zona_afectacion", "numero_integrantes", "observaciones"]) {
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
      asignaciones.push(
        `ubicacion = ST_SetSRID(ST_MakePoint($${valores.length - 1}, $${valores.length}), 4326)::geography`
      );
    }
  }
  valores.push(id);
  const resultado = await pool.query(
    `UPDATE grupos_familiares SET ${asignaciones.join(", ")} WHERE id = $${valores.length} RETURNING id`,
    valores
  );
  return resultado.rows[0] ? buscarPorId(id) : null;
}

module.exports = { crear, listar, buscarPorId, integrantes, actualizar };
