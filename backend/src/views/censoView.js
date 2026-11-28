// VISTA: forma en que se presentan grupos familiares y pacientes en las respuestas JSON.
const rellenar = (numero, largo) => String(numero).padStart(largo, "0");

function presentarGrupo(g) {
  return {
    id: g.id,
    codigo: g.codigo,
    albergue_id: g.albergue_id,
    zona_afectacion: g.zona_afectacion,
    latitud: g.latitud,
    longitud: g.longitud,
    numero_integrantes: g.numero_integrantes,
    integrantes_registrados: Number(g.integrantes_registrados),
    observaciones: g.observaciones,
    registrado_por: g.registrado_por,
    creado_en: g.creado_en,
  };
}

function presentarPaciente(p) {
  return {
    id: p.id,
    codigo: `P-${rellenar(p.numero, 3)}`,
    nombre_completo: `${p.nombres} ${p.apellidos}`.trim(),
    nombres: p.nombres,
    apellidos: p.apellidos,
    tipo_documento: p.tipo_documento,
    numero_documento: p.numero_documento,
    fecha_nacimiento: p.fecha_nacimiento,
    edad: p.edad,
    sexo: p.sexo,
    tipo_sangre: p.tipo_sangre,
    telefono: p.telefono,
    gestante: p.gestante,
    condiciones_previas: p.condiciones_previas,
    observaciones: p.observaciones,
    grupo_familiar_id: p.grupo_familiar_id,
    albergue_id: p.albergue_id,
    cerrado: p.cerrado,
    datos_ocr: p.datos_ocr,
    registrado_por: p.registrado_por,
    creado_en: p.creado_en,
    actualizado_en: p.actualizado_en,
    triage: presentarTriageVigente(p),
  };
}

// Resumen del triage mas reciente del paciente; null si nunca se evaluo.
// nivel_vigente es el confirmado por el medico o, si falta, el sugerido (confirmado=false).
function presentarTriageVigente(p) {
  if (!p.triage_id) return null;
  return {
    evaluacion_id: p.triage_id,
    nivel_sugerido: p.triage_sugerido,
    nivel_final: p.triage_final,
    nivel_vigente: p.triage_final || p.triage_sugerido,
    confirmado: p.triage_final !== null,
    evaluado_en: p.triage_evaluado_en,
    sindrome: p.triage_sindrome,
    estado_clinico: p.triage_estado_clinico,
    requiere_traslado: p.triage_requiere_traslado,
  };
}

module.exports = { presentarGrupo, presentarPaciente };
