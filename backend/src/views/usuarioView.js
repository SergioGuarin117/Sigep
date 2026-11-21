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

module.exports = { presentarUsuario };
