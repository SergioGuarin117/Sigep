const pool = require("../config/db");

function raiz(req, res) {
  res.json({ mensaje: "Backend de SIGEP activo" });
}

async function baseDeDatos(req, res) {
  try {
    await pool.query("SELECT 1");
    res.json({ mensaje: "Conexión con PostgreSQL correcta" });
  } catch (error) {
    console.error("Detalle del error de PostgreSQL:", error);
    res.status(500).json({ error: "No se pudo conectar con la base de datos" });
  }
}

module.exports = { raiz, baseDeDatos };
