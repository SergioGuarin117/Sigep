// SERVICIO: cliente del servicio de IA (ai-service). Nunca lanza errores: si la IA no esta disponible,
// devuelve { disponible: false, motivo } y el triage sigue funcionando con las reglas.
const env = require("../config/env");
const { NIVELES } = require("./triageReglas");

// Solo viajan datos clinicos: nunca nombres, documentos ni identificadores de la persona.
const CAMPOS_CLINICOS = [
  "frecuencia_cardiaca", "frecuencia_respiratoria", "presion_sistolica", "presion_diastolica",
  "temperatura", "saturacion_oxigeno", "conciencia", "sintomas", "estado_clinico", "requiere_traslado",
];

function construirEntrada(datos, paciente) {
  const entrada = {};
  for (const campo of CAMPOS_CLINICOS) {
    if (datos[campo] !== undefined && datos[campo] !== null) entrada[campo] = datos[campo];
  }
  if (typeof paciente.edad === "number") entrada.edad = paciente.edad;
  if (typeof paciente.gestante === "boolean") entrada.gestante = paciente.gestante;
  return entrada;
}

function respuestaValida(r) {
  return (
    r && NIVELES.includes(r.nivel) && typeof r.puntaje === "number" &&
    r.probabilidades && typeof r.probabilidades === "object" &&
    Array.isArray(r.factores) && Array.isArray(r.avisos) && typeof r.version_modelo === "string"
  );
}

async function predecirTriage(datos, paciente, { url = env.AI_SERVICE_URL, timeoutMs = env.AI_TIMEOUT_MS, activo = env.AI_ACTIVO } = {}) {
  if (!activo) return { disponible: false, motivo: "desactivada" };
  try {
    const respuesta = await fetch(`${url}/triage/predecir`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(construirEntrada(datos, paciente)),
      signal: AbortSignal.timeout(timeoutMs),
    });
    if (!respuesta.ok) {
      console.error(`Servicio de IA respondió ${respuesta.status}`);
      return { disponible: false, motivo: "error_servicio" };
    }
    const cuerpo = await respuesta.json();
    if (!respuestaValida(cuerpo)) {
      console.error("Servicio de IA devolvió una respuesta con formato inesperado");
      return { disponible: false, motivo: "respuesta_invalida" };
    }
    return {
      disponible: true,
      nivel: cuerpo.nivel,
      probabilidades: cuerpo.probabilidades,
      puntaje: cuerpo.puntaje,
      confianza: cuerpo.confianza || null,
      factores: cuerpo.factores,
      avisos: cuerpo.avisos,
      version: cuerpo.version_modelo,
    };
  } catch (error) {
    const agotado = error && (error.name === "TimeoutError" || error.name === "AbortError");
    console.error("No se pudo consultar el servicio de IA:", error && error.message);
    return { disponible: false, motivo: agotado ? "tiempo_agotado" : "sin_conexion" };
  }
}

// Estado del servicio para el panel de administracion.
async function estadoServicio({ url = env.AI_SERVICE_URL, timeoutMs = env.AI_TIMEOUT_MS, activo = env.AI_ACTIVO } = {}) {
  if (!activo) return { disponible: false, motivo: "desactivada" };
  try {
    const respuesta = await fetch(`${url}/health`, { signal: AbortSignal.timeout(timeoutMs) });
    if (!respuesta.ok) return { disponible: false, motivo: "error_servicio" };
    const cuerpo = await respuesta.json();
    return { disponible: true, modelo: cuerpo.modelo, metricas: cuerpo.metricas || null };
  } catch (error) {
    return { disponible: false, motivo: "sin_conexion" };
  }
}

module.exports = { predecirTriage, estadoServicio, construirEntrada };
