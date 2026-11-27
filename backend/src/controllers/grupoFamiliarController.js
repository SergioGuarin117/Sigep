// CONTROLADOR: grupos familiares del censo.
const grupoModel = require("../models/grupoFamiliarModel");
const { presentarGrupo, presentarPaciente } = require("../views/censoView");
const { responderErrorBD } = require("./erroresBD");

const MENSAJES_BD = { referencia: "El albergue indicado no existe" };

async function listar(req, res) {
  try {
    const { total, grupos } = await grupoModel.listar(req.filtros);
    res.json({
      total,
      limite: req.filtros.limite,
      desplazamiento: req.filtros.desplazamiento,
      grupos: grupos.map(presentarGrupo),
    });
  } catch (error) {
    console.error("Error al listar grupos familiares:", error);
    res.status(500).json({ error: "No se pudieron listar los grupos familiares" });
  }
}

async function obtener(req, res) {
  try {
    const grupo = await grupoModel.buscarPorId(req.params.id);
    if (!grupo) return res.status(404).json({ error: "Grupo familiar no encontrado" });
    const integrantes = await grupoModel.integrantes(grupo.id);
    res.json({
      grupo: {
        ...presentarGrupo(grupo),
        integrantes: integrantes.map((p) => ({
          id: p.id,
          codigo: presentarPaciente(p).codigo,
          nombre_completo: `${p.nombres} ${p.apellidos}`.trim(),
          cerrado: p.cerrado,
        })),
      },
    });
  } catch (error) {
    console.error("Error al consultar grupo familiar:", error);
    res.status(500).json({ error: "No se pudo consultar el grupo familiar" });
  }
}

async function crear(req, res) {
  try {
    const grupo = await grupoModel.crear(req.datos, req.usuario.id);
    res.status(201).json({ mensaje: "Grupo familiar registrado", grupo: presentarGrupo(grupo) });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al crear grupo familiar:", error);
    res.status(500).json({ error: "No se pudo registrar el grupo familiar" });
  }
}

async function actualizar(req, res) {
  try {
    const grupo = await grupoModel.actualizar(req.params.id, req.datos);
    if (!grupo) return res.status(404).json({ error: "Grupo familiar no encontrado" });
    res.json({ mensaje: "Grupo familiar actualizado", grupo: presentarGrupo(grupo) });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al actualizar grupo familiar:", error);
    res.status(500).json({ error: "No se pudo actualizar el grupo familiar" });
  }
}

module.exports = { listar, obtener, crear, actualizar };
