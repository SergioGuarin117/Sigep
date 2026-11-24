// MIDDLEWARE: restringe una ruta a ciertos roles. Va DESPUES de verificarToken.
//   router.get("/x", verificarToken, permitirRoles(ROLES.COORDINADOR_MUNICIPAL), controlador)
const { ROLES_VALIDOS } = require("../constants/roles");

function permitirRoles(...rolesPermitidos) {
  // Falla al arrancar (no en produccion) si se escribe mal un rol.
  rolesPermitidos.forEach((rol) => {
    if (!ROLES_VALIDOS.includes(rol)) {
      throw new Error(`permitirRoles: rol desconocido "${rol}"`);
    }
  });

  return (req, res, next) => {
    if (!req.usuario) {
      return res.status(401).json({ error: "Se requiere iniciar sesión" });
    }
    if (!rolesPermitidos.includes(req.usuario.rol)) {
      return res.status(403).json({ error: "No tienes permiso para esta acción" });
    }
    next();
  };
}

module.exports = { permitirRoles };
