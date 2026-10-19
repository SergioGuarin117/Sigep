const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const pool = require("./db");

const router = express.Router();

// Crear cuenta
router.post("/register", async (req, res) => {
  const { nombre, apellido, correo, contrasena } = req.body;

  if (!nombre || !apellido || !correo || !contrasena) {
    return res.status(400).json({
      error: "Nombre, apellido, correo y contraseña son obligatorios",
    });
  }

  try {
    const existente = await pool.query(
      "SELECT id FROM usuarios WHERE correo = $1",
      [correo]
    );

    if (existente.rows.length > 0) {
      return res.status(409).json({ error: "Ese correo ya está registrado" });
    }

    const hash = await bcrypt.hash(contrasena, 10);

    const resultado = await pool.query(
      `INSERT INTO usuarios (nombre, apellido, correo, contrasena_hash)
       VALUES ($1, $2, $3, $4)
       RETURNING id, nombre, apellido, correo, rol`,
      [nombre, apellido, correo, hash]
    );

    res.status(201).json({
      mensaje: "Cuenta creada correctamente",
      usuario: resultado.rows[0],
    });
  } catch (error) {
    console.error("Error al registrar:", error);
    res.status(500).json({ error: "No se pudo crear la cuenta" });
  }
});

// Iniciar sesión
router.post("/login", async (req, res) => {
  const { correo, contrasena } = req.body;

  if (!correo || !contrasena) {
    return res.status(400).json({ error: "Correo y contraseña son obligatorios" });
  }

  try {
    const resultado = await pool.query(
      `SELECT id, nombre, apellido, correo, contrasena_hash, rol
       FROM usuarios
       WHERE correo = $1 AND activo = TRUE`,
      [correo]
    );

    if (resultado.rows.length === 0) {
      return res.status(401).json({ error: "Correo o contraseña incorrectos" });
    }

    const usuario = resultado.rows[0];
    const coincide = await bcrypt.compare(contrasena, usuario.contrasena_hash);

    if (!coincide) {
      return res.status(401).json({ error: "Correo o contraseña incorrectos" });
    }

    const token = jwt.sign(
      { id: usuario.id, correo: usuario.correo, rol: usuario.rol },
      process.env.JWT_SECRET,
      { expiresIn: "1h" }
    );

    res.json({
      mensaje: "Inicio de sesión correcto",
      token,
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        apellido: usuario.apellido,
        correo: usuario.correo,
        rol: usuario.rol,
      },
    });
  } catch (error) {
    console.error("Error al iniciar sesión:", error);
    res.status(500).json({ error: "No se pudo iniciar sesión" });
  }
});

module.exports = router;