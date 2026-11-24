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

module.exports = { existeCorreo, crear, buscarActivoPorCorreo, buscarActivoPorId };
