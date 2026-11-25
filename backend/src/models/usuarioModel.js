// MODELO: acceso a datos de la tabla "usuarios". No conoce HTTP.
const pool = require("../config/db");
const { ROL_POR_DEFECTO } = require("../constants/roles");

async function existeCorreo(correo) {
  const resultado = await pool.query(
    "SELECT id FROM usuarios WHERE correo = $1",
    [correo]
  );
  return resultado.rows.length > 0;
}

async function crear({ nombre, apellido, correo, contrasenaHash }) {
  const resultado = await pool.query(
    `INSERT INTO usuarios (nombre, apellido, correo, contrasena_hash, rol)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING id, nombre, apellido, correo, rol`,
    [nombre, apellido, correo, contrasenaHash, ROL_POR_DEFECTO]
  );
  return resultado.rows[0];
}

async function buscarActivoPorCorreo(correo) {
  const resultado = await pool.query(
    `SELECT id, nombre, apellido, correo, contrasena_hash, rol
     FROM usuarios
     WHERE correo = $1 AND activo = TRUE`,
    [correo]
  );
  return resultado.rows[0] || null;
}

async function buscarActivoPorId(id) {
  const resultado = await pool.query(
    `SELECT id, nombre, apellido, correo, rol, albergue_id, ips_id
     FROM usuarios
     WHERE id = $1 AND activo = TRUE`,
    [id]
  );
  return resultado.rows[0] || null;
}

const COLUMNAS_VISIBLES =
  "id, nombre, apellido, correo, rol, albergue_id, ips_id, activo, creado_en";

// Campos que un coordinador puede modificar (lista blanca: se usa para armar el SQL).
const COLUMNAS_EDITABLES = ["rol", "albergue_id", "ips_id", "activo"];

function escaparLike(texto) {
  return texto.replace(/[\\%_]/g, "\\$&");
}

async function listar({ rol, activo, texto, limite, desplazamiento }) {
  const condiciones = [];
  const valores = [];

  if (rol) {
    valores.push(rol);
    condiciones.push(`rol = $${valores.length}`);
  }
  if (activo !== undefined) {
    valores.push(activo);
    condiciones.push(`activo = $${valores.length}`);
  }
  if (texto) {
    valores.push(`%${escaparLike(texto)}%`);
    const n = valores.length;
    condiciones.push(`(nombre ILIKE $${n} OR apellido ILIKE $${n} OR correo ILIKE $${n})`);
  }

  const where = condiciones.length ? `WHERE ${condiciones.join(" AND ")}` : "";
  valores.push(limite, desplazamiento);

  const resultado = await pool.query(
    `SELECT ${COLUMNAS_VISIBLES}, COUNT(*) OVER() AS total
     FROM usuarios
     ${where}
     ORDER BY creado_en DESC, id DESC
     LIMIT $${valores.length - 1} OFFSET $${valores.length}`,
    valores
  );

  const total = resultado.rows.length ? Number(resultado.rows[0].total) : 0;
  const usuarios = resultado.rows.map(({ total: _total, ...usuario }) => usuario);
  return { total, usuarios };
}

async function buscarPorId(id) {
  const resultado = await pool.query(
    `SELECT ${COLUMNAS_VISIBLES} FROM usuarios WHERE id = $1`,
    [id]
  );
  return resultado.rows[0] || null;
}

// cambios: objeto con algunas de COLUMNAS_EDITABLES. Devuelve el usuario o null.
async function actualizar(id, cambios) {
  const columnas = Object.keys(cambios).filter((c) => COLUMNAS_EDITABLES.includes(c));
  const asignaciones = columnas.map((c, i) => `${c} = $${i + 1}`);
  const valores = columnas.map((c) => cambios[c]);
  valores.push(id);

  const resultado = await pool.query(
    `UPDATE usuarios SET ${asignaciones.join(", ")}
     WHERE id = $${valores.length}
     RETURNING ${COLUMNAS_VISIBLES}`,
    valores
  );
  return resultado.rows[0] || null;
}

module.exports = {
  existeCorreo,
  crear,
  buscarActivoPorCorreo,
  buscarActivoPorId,
  listar,
  buscarPorId,
  actualizar,
  COLUMNAS_EDITABLES,
};
