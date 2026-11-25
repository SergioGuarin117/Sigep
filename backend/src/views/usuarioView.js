// VISTA: forma en que se presenta un usuario en las respuestas JSON.
// Nunca expone contrasena_hash.
function presentarUsuario(usuario) {
  return {
    id: usuario.id,
    nombre: usuario.nombre,
    apellido: usuario.apellido,
    correo: usuario.correo,
    rol: usuario.rol,
  };
}

// Version para el coordinador: incluye asignacion y estado de la cuenta.
function presentarUsuarioAdmin(usuario) {
  return {
    ...presentarUsuario(usuario),
    albergue_id: usuario.albergue_id,
    ips_id: usuario.ips_id,
    activo: usuario.activo,
    creado_en: usuario.creado_en,
  };
}

module.exports = { presentarUsuario, presentarUsuarioAdmin };
