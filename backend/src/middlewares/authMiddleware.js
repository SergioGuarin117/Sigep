// MIDDLEWARE: exige un token JWT valido en "Authorization: Bearer <token>".
// Si es valido deja los datos del usuario en req.usuario ({ id, correo, rol }).
const jwt = require("jsonwebtoken");
const { JWT_SECRET } = require("../config/env");

function verificarToken(req, res, next) {
  const cabecera = req.headers.authorization || "";
  const [tipo, token] = cabecera.split(" ");

  if (tipo !== "Bearer" || !token) {
    return res.status(401).json({ error: "Se requiere iniciar sesión" });
  }

  try {
    const datos = jwt.verify(token, JWT_SECRET);
    req.usuario = { id: datos.id, correo: datos.correo, rol: datos.rol };
    next();
  } catch (error) {
    const expirado = error.name === "TokenExpiredError";
    return res.status(401).json({
      error: expirado ? "La sesión expiró, inicia sesión de nuevo" : "Token inválido",
    });
  }
}

module.exports = { verificarToken };
