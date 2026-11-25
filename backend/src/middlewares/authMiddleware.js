// MIDDLEWARE: exige un token JWT valido en "Authorization: Bearer <token>".
// Ademas consulta la BD para usar el rol VIGENTE y rechazar cuentas desactivadas:
// asi un cambio de rol o una desactivacion hecha por el coordinador aplica de
// inmediato y no hasta que el token expire.
// Deja { id, correo, rol } en req.usuario.
const jwt = require("jsonwebtoken");
const { JWT_SECRET } = require("../config/env");
const usuarioModel = require("../models/usuarioModel");

async function verificarToken(req, res, next) {
  const cabecera = req.headers.authorization || "";
  const [tipo, token] = cabecera.split(" ");

  if (tipo !== "Bearer" || !token) {
    return res.status(401).json({ error: "Se requiere iniciar sesión" });
  }

  let datos;
  try {
    datos = jwt.verify(token, JWT_SECRET);
  } catch (error) {
    const expirado = error.name === "TokenExpiredError";
    return res.status(401).json({
      error: expirado ? "La sesión expiró, inicia sesión de nuevo" : "Token inválido",
    });
  }

  try {
    const usuario = await usuarioModel.buscarActivoPorId(datos.id);
    if (!usuario) {
      return res.status(401).json({ error: "La cuenta ya no está activa" });
    }
    req.usuario = { id: usuario.id, correo: usuario.correo, rol: usuario.rol };
    next();
  } catch (error) {
    console.error("Error al verificar la sesión:", error);
    res.status(500).json({ error: "No se pudo verificar la sesión" });
  }
}

module.exports = { verificarToken };
