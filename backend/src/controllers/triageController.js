// CONTROLADOR: evaluaciones de triage. El sistema SUGIERE (reglas); el medico CONFIRMA.
const triageModel = require("../models/triageModel");
const pacienteModel = require("../models/pacienteModel");
const { evaluarTriage } = require("../services/triageReglas");
const { presentarEvaluacion, presentarPendiente } = require("../views/triageView");

// Crea una evaluacion nueva para el paciente y calcula la sugerencia con las reglas.
async function evaluar(req, res) {
  try {
    const paciente = await pacienteModel.buscarPorId(req.params.id);
    if (!paciente) return res.status(404).json({ error: "Paciente no encontrado" });
    if (paciente.cerrado) {
      return res.status(409).json({ error: "El caso está cerrado: reábrelo para evaluar al paciente" });
    }

    const sugerencia = evaluarTriage({
      ...req.datos,
      edad: paciente.edad,
      gestante: paciente.gestante,
    });

    const evaluacion = await triageModel.crear({
      pacienteId: paciente.id,
      albergueId: paciente.albergue_id,
      usuarioId: req.usuario.id,
      datos: req.datos,
      sugerencia,
    });
    res.status(201).json({
      mensaje: "Evaluación registrada. Un médico debe confirmar el nivel.",
      evaluacion: presentarEvaluacion(evaluacion),
    });
  } catch (error) {
    console.error("Error al evaluar triage:", error);
    res.status(500).json({ error: "No se pudo registrar la evaluación" });
  }
}

async function historial(req, res) {
  try {
    if (!(await pacienteModel.existe(req.params.id))) {
      return res.status(404).json({ error: "Paciente no encontrado" });
    }
    const evaluaciones = await triageModel.listarPorPaciente(req.params.id);
    res.json({ total: evaluaciones.length, evaluaciones: evaluaciones.map(presentarEvaluacion) });
  } catch (error) {
    console.error("Error al consultar el historial de triage:", error);
    res.status(500).json({ error: "No se pudo consultar el historial" });
  }
}

async function pendientes(req, res) {
  try {
    const { total, pendientes: filas } = await triageModel.listarPendientes(req.filtros);
    res.json({
      total,
      limite: req.filtros.limite,
      desplazamiento: req.filtros.desplazamiento,
      pendientes: filas.map(presentarPendiente),
    });
  } catch (error) {
    console.error("Error al listar evaluaciones pendientes:", error);
    res.status(500).json({ error: "No se pudieron listar las evaluaciones pendientes" });
  }
}

async function confirmar(req, res) {
  try {
    const confirmada = await triageModel.confirmar(req.params.id, req.datos.nivel_final, req.usuario.id);
    if (!confirmada) {
      const existente = await triageModel.buscarPorId(req.params.id);
      if (!existente) return res.status(404).json({ error: "Evaluación no encontrada" });
      return res.status(409).json({
        error: "Esta evaluación ya fue confirmada. Si cambió el estado, registra una evaluación nueva",
      });
    }
    const evaluacion = await triageModel.buscarPorId(req.params.id);
    res.json({ mensaje: "Nivel de triage confirmado", evaluacion: presentarEvaluacion(evaluacion) });
  } catch (error) {
    console.error("Error al confirmar triage:", error);
    res.status(500).json({ error: "No se pudo confirmar el nivel de triage" });
  }
}

module.exports = { evaluar, historial, pendientes, confirmar };
