// CONTROLADOR (solo coordinador municipal): gestion de cuentas de usuario.
const usuarioModel = require("../models/usuarioModel");
const { presentarUsuarioAdmin } = require("../views/usuarioView");

const VIOLACION_FK = "23503"; // albergue_id o ips_id inexistente

async function listar(req, res) {
  try {
    const { total, usuarios } = await usuarioModel.listar(req.filtros);
    res.json({
      total,
      limite: req.filtros.limite,
      desplazamiento: req.filtros.desplazamiento,
      usuarios: usuarios.map(presentarUsuarioAdmin),
    });
  } catch (error) {
    console.error("Error al listar usuarios:", error);
    res.status(500).json({ error: "No se pudo listar los usuarios" });
  }
}

async function obtener(req, res) {
  try {
    const usuario = await usuarioModel.buscarPorId(req.params.id);
    if (!usuario) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }
    res.json({ usuario: presentarUsuarioAdmin(usuario) });
  } catch (error) {
    console.error("Error al consultar usuario:", error);
    res.status(500).json({ error: "No se pudo consultar el usuario" });
  }
}

async function actualizar(req, res) {
  const { cambios } = req;

  // Evita que el coordinador se quede sin acceso por accidente.
  if (Number(req.params.id) === Number(req.usuario.id) && ("rol" in cambios || "activo" in cambios)) {
    return res.status(403).json({
      error: "No puedes cambiar tu propio rol ni desactivar tu propia cuenta",
    });
  }

  try {
    const usuario = await usuarioModel.actualizar(req.params.id, cambios);
    if (!usuario) {
      return res.status(404).json({ error: "Usuario no encontrado" });
    }
    res.json({ mensaje: "Usuario actualizado", usuario: presentarUsuarioAdmin(usuario) });
  } catch (error) {
    if (error.code === VIOLACION_FK) {
      return res.status(400).json({ error: "El albergue o la IPS indicados no existen" });
    }
    console.error("Error al actualizar usuario:", error);
    res.status(500).json({ error: "No se pudo actualizar el usuario" });
  }
}

module.exports = { listar, obtener, actualizar };
