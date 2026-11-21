// CONTROLADOR: recibe la peticion, usa el modelo y responde con la vista.
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { JWT_SECRET, JWT_EXPIRA_EN } = require("../config/env");
const usuarioModel = require("../models/usuarioModel");
const { presentarUsuario } = require("../views/usuarioView");

// Crear cuenta
async function register(req, res) {
  const { nombre, apellido, correo, contrasena } = req.body;

  if (!nombre || !apellido || !correo || !contrasena) {
    return res.status(400).json({
      error: "Nombre, apellido, correo y contraseña son obligatorios",
    });
  }

  try {
    if (await usuarioModel.existeCorreo(correo)) {
      return res.status(409).json({ error: "Ese correo ya está registrado" });
    }

    const contrasenaHash = await bcrypt.hash(contrasena, 10);
    const usuario = await usuarioModel.crear({
      nombre,
      apellido,
      correo,
      contrasenaHash,
    });

    res.status(201).json({
      mensaje: "Cuenta creada correctamente",
      usuario: presentarUsuario(usuario),
    });
  } catch (error) {
    console.error("Error al registrar:", error);
    res.status(500).json({ error: "No se pudo crear la cuenta" });
  }
}

// Iniciar sesión
async function login(req, res) {
  const { correo, contrasena } = req.body;

  if (!correo || !contrasena) {
    return res.status(400).json({ error: "Correo y contraseña son obligatorios" });
  }

  try {
    const usuario = await usuarioModel.buscarActivoPorCorreo(correo);

    if (!usuario) {
      return res.status(401).json({ error: "Correo o contraseña incorrectos" });
    }

    const coincide = await bcrypt.compare(contrasena, usuario.contrasena_hash);

    if (!coincide) {
      return res.status(401).json({ error: "Correo o contraseña incorrectos" });
    }

    const token = jwt.sign(
      { id: usuario.id, correo: usuario.correo, rol: usuario.rol },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRA_EN }
    );

    res.json({
      mensaje: "Inicio de sesión correcto",
      token,
      usuario: presentarUsuario(usuario),
    });
  } catch (error) {
    console.error("Error al iniciar sesión:", error);
    res.status(500).json({ error: "No se pudo iniciar sesión" });
  }
}

module.exports = { register, login };
