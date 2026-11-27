// CONTROLADOR: albergues. Leer lo puede cualquier usuario con sesion; crear y editar, el coordinador.
const albergueModel = require("../models/albergueModel");
const { presentarAlbergue } = require("../views/albergueView");
const { responderErrorBD } = require("./erroresBD");

const MENSAJES_BD = { unica: "Ya existe un albergue con ese nombre" };

async function listar(req, res) {
  try {
    const albergues = await albergueModel.listar(req.filtros);
    res.json({ total: albergues.length, albergues: albergues.map(presentarAlbergue) });
  } catch (error) {
    console.error("Error al listar albergues:", error);
    res.status(500).json({ error: "No se pudieron listar los albergues" });
  }
}

async function obtener(req, res) {
  try {
    const albergue = await albergueModel.buscarPorId(req.params.id);
    if (!albergue) return res.status(404).json({ error: "Albergue no encontrado" });
    res.json({ albergue: presentarAlbergue(albergue) });
  } catch (error) {
    console.error("Error al consultar albergue:", error);
    res.status(500).json({ error: "No se pudo consultar el albergue" });
  }
}

async function crear(req, res) {
  try {
    const albergue = await albergueModel.crear(req.datos);
    res.status(201).json({ mensaje: "Albergue creado", albergue: presentarAlbergue(albergue) });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al crear albergue:", error);
    res.status(500).json({ error: "No se pudo crear el albergue" });
  }
}

async function actualizar(req, res) {
  try {
    const albergue = await albergueModel.actualizar(req.params.id, req.datos);
    if (!albergue) return res.status(404).json({ error: "Albergue no encontrado" });
    res.json({ mensaje: "Albergue actualizado", albergue: presentarAlbergue(albergue) });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al actualizar albergue:", error);
    res.status(500).json({ error: "No se pudo actualizar el albergue" });
  }
}

module.exports = { listar, obtener, crear, actualizar };
