// Pruebas del motor de reglas de triage. Ejecutar con: npm test
const test = require("node:test");
const assert = require("node:assert/strict");
const { evaluarTriage } = require("../src/services/triageReglas");

const estable = {
  frecuencia_cardiaca: 80, frecuencia_respiratoria: 16, presion_sistolica: 120, presion_diastolica: 80,
  temperatura: 36.8, saturacion_oxigeno: 98, conciencia: "alerta", sintomas: [], edad: 30,
};
const con = (cambios) => ({ ...estable, ...cambios });
const reglas = (r) => r.motivos.map((m) => m.regla);

test("paciente estable con signos completos es verde y puntaje 0", () => {
  const r = evaluarTriage(estable);
  assert.equal(r.nivel, "verde");
  assert.equal(r.puntaje, 0);
  assert.equal(r.confianza, "alta");
  assert.deepEqual(r.datos_faltantes, []);
  assert.deepEqual(r.avisos, []);
});

test("saturación menor a 90 es rojo", () => {
  const r = evaluarTriage(con({ saturacion_oxigeno: 85 }));
  assert.equal(r.nivel, "rojo");
  assert.ok(reglas(r).includes("SATURACION_CRITICA"));
});

test("saturación entre 90 y 94 es amarillo", () => {
  assert.equal(evaluarTriage(con({ saturacion_oxigeno: 92 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ saturacion_oxigeno: 94 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ saturacion_oxigeno: 95 })).nivel, "verde");
});

test("conciencia: voz amarillo, dolor rojo, inconsciente con signos rojo", () => {
  assert.equal(evaluarTriage(con({ conciencia: "voz" })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ conciencia: "dolor" })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ conciencia: "inconsciente" })).nivel, "rojo");
});

test("inconsciente sin pulso o sin respiración es negro y avisa al médico", () => {
  const sinPulso = evaluarTriage(con({ conciencia: "inconsciente", frecuencia_cardiaca: 0 }));
  assert.equal(sinPulso.nivel, "negro");
  assert.ok(sinPulso.avisos.some((a) => a.includes("médico")));
  assert.equal(evaluarTriage(con({ conciencia: "inconsciente", frecuencia_respiratoria: 0 })).nivel, "negro");
});

test("pulso en cero estando alerta no es negro (dato contradictorio) sino rojo", () => {
  const r = evaluarTriage(con({ frecuencia_cardiaca: 0 }));
  assert.equal(r.nivel, "rojo");
  assert.ok(reglas(r).includes("PULSO_CRITICO"));
});

test("hipotensión: <=89 rojo y 90-99 amarillo en adultos", () => {
  assert.equal(evaluarTriage(con({ presion_sistolica: 85 })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ presion_sistolica: 95 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ presion_sistolica: 185 })).nivel, "amarillo");
});

test("temperatura: 38,5 amarillo, 40 rojo, 34,5 rojo", () => {
  assert.equal(evaluarTriage(con({ temperatura: 38.5 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ temperatura: 40 })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ temperatura: 34.5 })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ temperatura: 38.4 })).nivel, "verde");
});

test("los umbrales respiratorios cambian en menores de 12 años", () => {
  assert.equal(evaluarTriage(con({ frecuencia_respiratoria: 35, edad: 8 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ frecuencia_respiratoria: 35, edad: 30 })).nivel, "rojo");
});

test("síntomas se reconocen sin importar tildes ni mayúsculas", () => {
  const r = evaluarTriage(con({ sintomas: ["DESHIDRATACION", "herida visible"] }));
  assert.equal(r.nivel, "amarillo");
  assert.ok(reglas(r).includes("DESHIDRATACION") && reglas(r).includes("HERIDA_VISIBLE"));
});

test("dificultad para respirar es amarillo y con hipoxia sube a rojo", () => {
  assert.equal(evaluarTriage(con({ sintomas: ["Dificultad para respirar"] })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ sintomas: ["Dificultad para respirar"], saturacion_oxigeno: 92 })).nivel, "rojo");
});

test("diarrea con vómito es amarillo; diarrea sola en adulto es verde", () => {
  assert.equal(evaluarTriage(con({ sintomas: ["Diarrea", "Vómito"] })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ sintomas: ["Diarrea"] })).nivel, "verde");
});

test("menores de 5 y mayores de 64 con fiebre o diarrea son amarillo", () => {
  assert.equal(evaluarTriage(con({ sintomas: ["Diarrea"], edad: 3 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ sintomas: ["Fiebre"], edad: 70 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ sintomas: ["Fiebre"], edad: 30 })).nivel, "verde");
});

test("gestante: presión 140/90 amarillo y 160/110 rojo", () => {
  assert.equal(evaluarTriage(con({ gestante: true, presion_sistolica: 145 })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ gestante: true, presion_sistolica: 165 })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ gestante: false, presion_sistolica: 145 })).nivel, "verde");
});

test("estado clínico y traslado marcados por el personal suben el nivel", () => {
  assert.equal(evaluarTriage(con({ estado_clinico: "grave" })).nivel, "amarillo");
  assert.equal(evaluarTriage(con({ estado_clinico: "critico" })).nivel, "rojo");
  assert.equal(evaluarTriage(con({ requiere_traslado: true })).nivel, "amarillo");
});

test("gana el nivel más grave y el puntaje se acumula con tope de 100", () => {
  const r = evaluarTriage(con({
    saturacion_oxigeno: 80, conciencia: "dolor", presion_sistolica: 70, temperatura: 41,
    frecuencia_respiratoria: 40,
  }));
  assert.equal(r.nivel, "rojo");
  assert.equal(r.puntaje, 100);
});

test("sin signos vitales: confianza baja y aviso si da verde", () => {
  const r = evaluarTriage({ conciencia: "alerta", sintomas: [] });
  assert.equal(r.nivel, "verde");
  assert.equal(r.confianza, "baja");
  assert.equal(r.datos_faltantes.length, 6);
  assert.equal(r.avisos.length, 1);
});

test("datos parciales: confianza media y los vacíos no activan reglas", () => {
  const r = evaluarTriage({ saturacion_oxigeno: 97, temperatura: 37, conciencia: "alerta" });
  assert.equal(r.confianza, "media");
  assert.equal(r.nivel, "verde");
});

test("la respuesta indica la versión de las reglas", () => {
  assert.equal(evaluarTriage(estable).version, "reglas-v1");
});
