// SERVICIO: motor de reglas de triage (Verde, Amarillo, Rojo, Negro).
//
// Es una funcion pura: recibe signos vitales y sintomas y devuelve el nivel SUGERIDO con los
// motivos que pesaron. No guarda nada ni decide: el nivel final lo confirma siempre el medico.
// Los umbrales estan simplificados a partir de criterios de triage de emergencia (START/ABCDE)
// y deben ser validados por el equipo medico antes de un uso real.
const VERSION = "reglas-v1";

const NIVELES = Object.freeze(["verde", "amarillo", "rojo", "negro"]);
const PESO = Object.freeze({ amarillo: 15, rojo: 40, negro: 100 });
const SIGNOS_VITALES = Object.freeze([
  "frecuencia_cardiaca",
  "frecuencia_respiratoria",
  "presion_sistolica",
  "presion_diastolica",
  "temperatura",
  "saturacion_oxigeno",
]);

const normalizar = (texto) =>
  String(texto).normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();

const medido = (valor) => typeof valor === "number" && Number.isFinite(valor);
const fuera = (valor, minimo, maximo) => medido(valor) && (valor < minimo || valor > maximo);
const entre = (valor, minimo, maximo) => medido(valor) && valor >= minimo && valor <= maximo;

// Umbrales segun el grupo de edad (menores de 12 anos o resto).
function umbrales(edad) {
  const nino = medido(edad) && edad < 12;
  return nino
    ? {
        frRojo: [10, 50], frAmarillo: [31, 50],
        fcRojo: [60, 180], fcAmarillo: [131, 180],
        pasRojoMax: 69, pasAmarilloMax: 79,
      }
    : {
        frRojo: [8, 30], frAmarillo: [21, 30],
        fcRojo: [40, 140], fcAmarillo: [111, 140],
        pasRojoMax: 89, pasAmarilloMax: 99,
      };
}

// Cada regla: id estable, nivel que sugiere, texto para el medico y condicion.
function construirReglas(e, u) {
  const sintomas = new Set((e.sintomas || []).map(normalizar));
  const tiene = (s) => sintomas.has(normalizar(s));
  const sinVitales = (e.frecuencia_cardiaca === 0 || e.frecuencia_respiratoria === 0);
  const edad = medido(e.edad) ? e.edad : null;
  const vulnerable = edad !== null && (edad < 5 || edad >= 65);

  return [
    // ---- Negro
    {
      id: "SIN_SIGNOS_VITALES", nivel: "negro",
      descripcion: "Inconsciente y sin pulso o sin respiración",
      aplica: sinVitales && e.conciencia === "inconsciente",
    },
    // ---- Rojo (atencion inmediata)
    {
      id: "CONCIENCIA_GRAVE", nivel: "rojo",
      descripcion: "Responde solo al dolor o está inconsciente",
      aplica: e.conciencia === "dolor" || e.conciencia === "inconsciente",
    },
    {
      id: "SATURACION_CRITICA", nivel: "rojo",
      descripcion: "Saturación de oxígeno menor a 90 %",
      aplica: medido(e.saturacion_oxigeno) && e.saturacion_oxigeno < 90,
    },
    {
      id: "RESPIRACION_CRITICA", nivel: "rojo",
      descripcion: "Frecuencia respiratoria fuera del rango seguro",
      aplica: fuera(e.frecuencia_respiratoria, u.frRojo[0], u.frRojo[1]),
    },
    {
      id: "PULSO_CRITICO", nivel: "rojo",
      descripcion: "Frecuencia cardíaca fuera del rango seguro",
      aplica: fuera(e.frecuencia_cardiaca, u.fcRojo[0], u.fcRojo[1]),
    },
    {
      id: "HIPOTENSION_SEVERA", nivel: "rojo",
      descripcion: "Presión sistólica muy baja (posible choque)",
      aplica: medido(e.presion_sistolica) && e.presion_sistolica <= u.pasRojoMax,
    },
    {
      id: "TEMPERATURA_CRITICA", nivel: "rojo",
      descripcion: "Temperatura de 40 °C o más, o de 35 °C o menos",
      aplica: medido(e.temperatura) && (e.temperatura >= 40 || e.temperatura <= 35),
    },
    {
      id: "GESTANTE_PRESION_SEVERA", nivel: "rojo",
      descripcion: "Gestante con presión arterial severamente elevada",
      aplica: e.gestante === true && (e.presion_sistolica >= 160 || e.presion_diastolica >= 110),
    },
    {
      id: "DIFICULTAD_RESPIRAR_HIPOXIA", nivel: "rojo",
      descripcion: "Dificultad para respirar con saturación menor a 94 %",
      aplica: tiene("Dificultad para respirar") && medido(e.saturacion_oxigeno) && e.saturacion_oxigeno < 94,
    },
    {
      id: "ESTADO_CRITICO", nivel: "rojo",
      descripcion: "El personal registró estado clínico crítico",
      aplica: e.estado_clinico === "critico",
    },
    // ---- Amarillo (prioritaria)
    {
      id: "CONCIENCIA_ALTERADA", nivel: "amarillo",
      descripcion: "Responde solo a la voz",
      aplica: e.conciencia === "voz",
    },
    {
      id: "SATURACION_BAJA", nivel: "amarillo",
      descripcion: "Saturación de oxígeno entre 90 y 94 %",
      aplica: entre(e.saturacion_oxigeno, 90, 94),
    },
    {
      id: "RESPIRACION_ALTERADA", nivel: "amarillo",
      descripcion: "Frecuencia respiratoria elevada",
      aplica: entre(e.frecuencia_respiratoria, u.frAmarillo[0], u.frAmarillo[1]),
    },
    {
      id: "PULSO_ALTERADO", nivel: "amarillo",
      descripcion: "Frecuencia cardíaca alterada",
      // Taquicardia moderada o bradicardia leve (justo por encima del limite rojo inferior).
      aplica:
        entre(e.frecuencia_cardiaca, u.fcAmarillo[0], u.fcAmarillo[1]) ||
        entre(e.frecuencia_cardiaca, u.fcRojo[0], u.fcRojo[0] + 10),
    },
    {
      id: "PRESION_ALTERADA", nivel: "amarillo",
      descripcion: "Presión sistólica baja (90-99) o muy alta (180 o más)",
      aplica:
        entre(e.presion_sistolica, u.pasRojoMax + 1, u.pasAmarilloMax) || (medido(e.presion_sistolica) && e.presion_sistolica >= 180),
    },
    {
      id: "FIEBRE_ALTA", nivel: "amarillo",
      descripcion: "Temperatura de 38,5 °C o más",
      aplica: medido(e.temperatura) && e.temperatura >= 38.5 && e.temperatura < 40,
    },
    {
      id: "DIFICULTAD_RESPIRAR", nivel: "amarillo",
      descripcion: "Refiere dificultad para respirar",
      aplica: tiene("Dificultad para respirar"),
    },
    {
      id: "DESHIDRATACION", nivel: "amarillo",
      descripcion: "Signos de deshidratación",
      aplica: tiene("Deshidratación"),
    },
    {
      id: "HERIDA_VISIBLE", nivel: "amarillo",
      descripcion: "Herida visible",
      aplica: tiene("Herida visible"),
    },
    {
      id: "DIARREA_Y_VOMITO", nivel: "amarillo",
      descripcion: "Diarrea con vómito: riesgo de deshidratación",
      aplica: tiene("Diarrea") && tiene("Vómito"),
    },
    {
      id: "VULNERABLE_CON_FIEBRE_O_DIARREA", nivel: "amarillo",
      descripcion: "Menor de 5 años o mayor de 64 con fiebre o diarrea",
      aplica: vulnerable && (tiene("Fiebre") || tiene("Diarrea") || (medido(e.temperatura) && e.temperatura >= 38)),
    },
    {
      id: "GESTANTE_PRESION_ELEVADA", nivel: "amarillo",
      descripcion: "Gestante con presión arterial elevada",
      aplica: e.gestante === true && (e.presion_sistolica >= 140 || e.presion_diastolica >= 90),
    },
    {
      id: "ESTADO_GRAVE", nivel: "amarillo",
      descripcion: "El personal registró estado clínico grave",
      aplica: e.estado_clinico === "grave",
    },
    {
      id: "REQUIERE_TRASLADO", nivel: "amarillo",
      descripcion: "El personal marcó que requiere traslado",
      aplica: e.requiere_traslado === true,
    },
  ];
}

/**
 * Evalua una entrada y devuelve:
 *   nivel          verde | amarillo | rojo | negro (el mas grave de las reglas activadas)
 *   puntaje        0 a 100 (suma de pesos, solo orienta)
 *   motivos        reglas activadas: { regla, nivel, descripcion }
 *   datos_faltantes  signos vitales sin medir
 *   confianza      alta | media | baja segun cuantos signos vitales se midieron
 *   avisos         textos de advertencia para quien confirma
 */
function evaluarTriage(entrada) {
  const u = umbrales(entrada.edad);
  const activadas = construirReglas(entrada, u).filter((regla) => regla.aplica);

  const nivel = activadas.reduce(
    (max, regla) => (NIVELES.indexOf(regla.nivel) > NIVELES.indexOf(max) ? regla.nivel : max),
    "verde"
  );
  const puntaje = Math.min(100, activadas.reduce((suma, regla) => suma + PESO[regla.nivel], 0));

  const datosFaltantes = SIGNOS_VITALES.filter((campo) => !medido(entrada[campo]));
  const medidos = SIGNOS_VITALES.length - datosFaltantes.length;
  const confianza = medidos >= 4 ? "alta" : medidos >= 2 ? "media" : "baja";

  const avisos = [];
  if (confianza === "baja" && nivel === "verde") {
    avisos.push("Faltan signos vitales: el nivel verde no descarta gravedad. Toma las medidas pendientes.");
  }
  if (nivel === "negro") {
    avisos.push("Nivel negro: solo un médico puede confirmar el fallecimiento o la condición expectante.");
  }

  return {
    nivel,
    puntaje,
    motivos: activadas.map(({ id, nivel: n, descripcion }) => ({ regla: id, nivel: n, descripcion })),
    datos_faltantes: datosFaltantes,
    confianza,
    avisos,
    version: VERSION,
  };
}

module.exports = { evaluarTriage, VERSION, NIVELES, SIGNOS_VITALES };
