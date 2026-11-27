// CONTROLADOR: pacientes del censo epidemiologico.
const pacienteModel = require("../models/pacienteModel");
const historiaModel = require("../models/historiaModel");
const { presentarPaciente } = require("../views/censoView");
const { responderErrorBD } = require("./erroresBD");
const { ROLES } = require("../constants/roles");

const MENSAJES_BD = {
  unica: "Ya existe un paciente con ese documento",
  referencia: "El albergue o el grupo familiar indicados no existen",
};

// Cerrar o reabrir un caso es una decision clinica/administrativa: no la toma el brigadista.
const ROLES_CIERRE = [ROLES.MEDICO_TRIAGE, ROLES.ADMIN_ALBERGUE, ROLES.COORDINADOR_MUNICIPAL];

async function listar(req, res) {
  try {
    const { total, pacientes } = await pacienteModel.listar(req.filtros);
    res.json({
      total,
      limite: req.filtros.limite,
      desplazamiento: req.filtros.desplazamiento,
      pacientes: pacientes.map(presentarPaciente),
    });
  } catch (error) {
    console.error("Error al listar pacientes:", error);
    res.status(500).json({ error: "No se pudieron listar los pacientes" });
  }
}

async function obtener(req, res) {
  try {
    const paciente = await pacienteModel.buscarPorId(req.params.id);
    if (!paciente) return res.status(404).json({ error: "Paciente no encontrado" });
    const historia = await historiaModel.obtener(paciente.id);
    res.json({ paciente: { ...presentarPaciente(paciente), historia_clinica: historia } });
  } catch (error) {
    console.error("Error al consultar paciente:", error);
    res.status(500).json({ error: "No se pudo consultar el paciente" });
  }
}

async function crear(req, res) {
  try {
    const id = await pacienteModel.crearCompleto(req.datos, req.usuario.id, req.historia);
    const paciente = await pacienteModel.buscarPorId(id);
    const historia = await historiaModel.obtener(id);
    res.status(201).json({
      mensaje: "Paciente registrado",
      paciente: { ...presentarPaciente(paciente), historia_clinica: historia },
    });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al crear paciente:", error);
    res.status(500).json({ error: "No se pudo registrar el paciente" });
  }
}

async function actualizar(req, res) {
  if ("cerrado" in req.datos && !ROLES_CIERRE.includes(req.usuario.rol)) {
    return res.status(403).json({ error: "Tu rol no puede cerrar ni reabrir casos" });
  }
  try {
    const paciente = await pacienteModel.actualizar(req.params.id, req.datos);
    if (!paciente) return res.status(404).json({ error: "Paciente no encontrado" });
    res.json({ mensaje: "Paciente actualizado", paciente: presentarPaciente(paciente) });
  } catch (error) {
    if (responderErrorBD(error, res, MENSAJES_BD)) return;
    console.error("Error al actualizar paciente:", error);
    res.status(500).json({ error: "No se pudo actualizar el paciente" });
  }
}

// ---- Historia clinica: un registro a la vez (morbilidad, medicamento o vacuna).
async function agregarHistoria(req, res) {
  try {
    if (!(await pacienteModel.existe(req.params.id))) {
      return res.status(404).json({ error: "Paciente no encontrado" });
    }
    const registro = await historiaModel.agregar(req.params.tipo, req.params.id, req.datos);
    res.status(201).json({ mensaje: "Registro agregado", registro });
  } catch (error) {
    console.error("Error al agregar a la historia clinica:", error);
    res.status(500).json({ error: "No se pudo agregar el registro" });
  }
}

async function eliminarHistoria(req, res) {
  try {
    const eliminado = await historiaModel.eliminar(req.params.tipo, req.params.id, req.params.itemId);
    if (!eliminado) return res.status(404).json({ error: "Registro no encontrado" });
    res.json({ mensaje: "Registro eliminado" });
  } catch (error) {
    console.error("Error al eliminar de la historia clinica:", error);
    res.status(500).json({ error: "No se pudo eliminar el registro" });
  }
}

module.exports = { listar, obtener, crear, actualizar, agregarHistoria, eliminarHistoria, ROLES_CIERRE };
