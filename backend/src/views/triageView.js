// VISTA: forma en que se presenta una evaluacion de triage en las respuestas JSON.
function presentarEvaluacion(e) {
  const explicacion = e.explicacion_ia || {};
  return {
    id: e.id,
    paciente_id: e.paciente_id,
    albergue_id: e.albergue_id,
    evaluado_por: e.evaluado_por,
    evaluado_por_nombre: e.evaluado_por_nombre,
    evaluado_en: e.evaluado_en,
    signos_vitales: {
      frecuencia_cardiaca: e.frecuencia_cardiaca,
      frecuencia_respiratoria: e.frecuencia_respiratoria,
      presion_sistolica: e.presion_sistolica,
      presion_diastolica: e.presion_diastolica,
      temperatura: e.temperatura,
      saturacion_oxigeno: e.saturacion_oxigeno,
      conciencia: e.conciencia,
    },
    sintomas: e.sintomas,
    sindrome: e.sindrome,
    estado_clinico: e.estado_clinico,
    requiere_traslado: e.requiere_traslado,
    notas: e.notas,
    sugerencia: {
      nivel: e.nivel_sugerido_ia,
      puntaje: e.puntaje_ia,
      version: e.version_modelo,
      confianza: explicacion.confianza || null,
      motivos: explicacion.motivos || [],
      datos_faltantes: explicacion.datos_faltantes || [],
      avisos: explicacion.avisos || [],
    },
    // La sugerencia no es una decision: hasta que un medico confirma, nivel_final es null.
    pendiente_confirmacion: e.nivel_final === null,
    decision:
      e.nivel_final === null
        ? null
        : {
            nivel_final: e.nivel_final,
            confirmado_por: e.confirmado_por,
            confirmado_por_nombre: e.confirmado_por_nombre,
            confirmado_en: e.confirmado_en,
          },
  };
}

// Elemento de la cola de pendientes: la evaluacion mas datos basicos del paciente.
function presentarPendiente(fila) {
  return {
    evaluacion: presentarEvaluacion(fila),
    paciente: {
      id: fila.paciente_id,
      codigo: `P-${String(fila.paciente_numero).padStart(3, "0")}`,
      nombre_completo: `${fila.paciente_nombres} ${fila.paciente_apellidos}`.trim(),
      edad: fila.paciente_edad,
      albergue_id: fila.paciente_albergue_id,
    },
  };
}

module.exports = { presentarEvaluacion, presentarPendiente };
