// MIDDLEWARE: valida y limpia el cuerpo de registro e inicio de sesion.
const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_CONTRASENA = 6;

function texto(valor) {
  return typeof valor === "string" ? valor.trim() : "";
}

function validarRegistro(req, res, next) {
  const nombre = texto(req.body.nombre);
  const apellido = texto(req.body.apellido);
  const correo = texto(req.body.correo);
  const contrasena = typeof req.body.contrasena === "string" ? req.body.contrasena : "";

  if (!nombre || !apellido || !correo || !contrasena) {
    return res.status(400).json({
      error: "Nombre, apellido, correo y contraseña son obligatorios",
    });
  }
  if (nombre.length > 100 || apellido.length > 100) {
    return res.status(400).json({ error: "Nombre y apellido admiten máximo 100 caracteres" });
  }
  if (!CORREO_VALIDO.test(correo) || correo.length > 255) {
    return res.status(400).json({ error: "El correo no es válido" });
  }
  if (contrasena.length < MIN_CONTRASENA) {
    return res.status(400).json({
      error: `La contraseña debe tener al menos ${MIN_CONTRASENA} caracteres`,
    });
  }

  req.body = { nombre, apellido, correo, contrasena };
  next();
}

function validarLogin(req, res, next) {
  const correo = texto(req.body.correo);
  const contrasena = typeof req.body.contrasena === "string" ? req.body.contrasena : "";

  if (!correo || !contrasena) {
    return res.status(400).json({ error: "Correo y contraseña son obligatorios" });
  }

  req.body = { correo, contrasena };
  next();
}

module.exports = { validarRegistro, validarLogin };
