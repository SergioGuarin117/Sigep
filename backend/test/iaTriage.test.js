// Pruebas del cliente del servicio de IA con un servidor falso (no necesitan Docker ni base de datos).
const test = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");
const { predecirTriage, estadoServicio, construirEntrada } = require("../src/services/iaTriage");
const { presentarEvaluacion } = require("../src/views/triageView");

const RESPUESTA_OK = {
  nivel: "rojo", probabilidades: { verde: 0, amarillo: 0.05, rojo: 0.95, negro: 0 }, puntaje: 73,
  confianza: "alta", factores: [{ variable: "saturacion_oxigeno", descripcion: "Saturación de oxígeno 85 %", impacto: 0.09 }],
  avisos: ["aviso"], version_modelo: "rf-sintetico-v1",
};

// Levanta un servidor falso que responde con `manejador(req, cuerpo)`.
async function conServidor(manejador, prueba) {
  const recibidos = [];
  const servidor = http.createServer((req, res) => {
    let cuerpo = "";
    req.on("data", (d) => (cuerpo += d));
    req.on("end", () => {
      recibidos.push({ url: req.url, cuerpo: cuerpo ? JSON.parse(cuerpo) : null });
      manejador(req, res);
    });
  });
  await new Promise((r) => servidor.listen(0, "127.0.0.1", r));
  const url = `http://127.0.0.1:${servidor.address().port}`;
  try {
    await prueba(url, recibidos);
  } finally {
    servidor.closeAllConnections();
    await new Promise((r) => servidor.close(r));
  }
}

const json = (estado, cuerpo) => (_req, res) => {
  res.writeHead(estado, { "Content-Type": "application/json" });
  res.end(JSON.stringify(cuerpo));
};

const paciente = { edad: 40, gestante: false, nombres: "Ana", apellidos: "Pérez", documento: "123" };
const datos = { saturacion_oxigeno: 85, conciencia: "dolor", sintomas: ["Fiebre"], notas: "texto libre", sindrome: "febril" };

test("construirEntrada envia solo datos clinicos (sin nombres, documento ni notas)", () => {
  const e = construirEntrada(datos, paciente);
  assert.deepEqual(e, { saturacion_oxigeno: 85, conciencia: "dolor", sintomas: ["Fiebre"], edad: 40, gestante: false });
  assert.equal("nombres" in e, false);
  assert.equal("notas" in e, false);
});

test("predecirTriage devuelve la sugerencia cuando el servicio responde bien", async () => {
  await conServidor(json(200, RESPUESTA_OK), async (url, recibidos) => {
    const r = await predecirTriage(datos, paciente, { url, activo: true });
    assert.equal(r.disponible, true);
    assert.equal(r.nivel, "rojo");
    assert.equal(r.version, "rf-sintetico-v1");
    assert.equal(recibidos[0].url, "/triage/predecir");
    assert.equal(recibidos[0].cuerpo.nombres, undefined);
  });
});

test("predecirTriage no falla si el servicio responde con error", async () => {
  await conServidor(json(500, { detail: "x" }), async (url) => {
    assert.deepEqual(await predecirTriage(datos, paciente, { url, activo: true }), { disponible: false, motivo: "error_servicio" });
  });
});

test("predecirTriage rechaza respuestas con formato inesperado", async () => {
  await conServidor(json(200, { nivel: "azul" }), async (url) => {
    assert.equal((await predecirTriage(datos, paciente, { url, activo: true })).motivo, "respuesta_invalida");
  });
});

test("predecirTriage informa tiempo agotado", async () => {
  await conServidor(() => {}, async (url) => {
    const r = await predecirTriage(datos, paciente, { url, timeoutMs: 150, activo: true });
    assert.deepEqual(r, { disponible: false, motivo: "tiempo_agotado" });
  });
});

test("predecirTriage informa sin conexion cuando nadie escucha", async () => {
  const r = await predecirTriage(datos, paciente, { url: "http://127.0.0.1:1", timeoutMs: 500, activo: true });
  assert.equal(r.disponible, false);
  assert.equal(r.motivo, "sin_conexion");
});

test("predecirTriage no llama al servicio si esta desactivada", async () => {
  await conServidor(json(200, RESPUESTA_OK), async (url, recibidos) => {
    assert.equal((await predecirTriage(datos, paciente, { url, activo: false })).motivo, "desactivada");
    assert.equal(recibidos.length, 0);
  });
});

test("estadoServicio lee /health", async () => {
  await conServidor(json(200, { estado: "ok", modelo: "rf-sintetico-v1", metricas: { exactitud: 0.93 } }), async (url) => {
    const r = await estadoServicio({ url, activo: true });
    assert.equal(r.disponible, true);
    assert.equal(r.modelo, "rf-sintetico-v1");
  });
  assert.equal((await estadoServicio({ url: "http://127.0.0.1:1", timeoutMs: 300, activo: true })).disponible, false);
});

// ---- vista ----
const fila = (explicacion, nivelReglas = "rojo") => ({
  id: 1, nivel_sugerido_ia: nivelReglas, puntaje_ia: 40, version_modelo: "reglas-v1", nivel_final: null,
  explicacion_ia: explicacion,
});

test("la vista marca si la IA coincide con las reglas", () => {
  const igual = presentarEvaluacion(fila({ ia: { disponible: true, ...RESPUESTA_OK, version: "v1" } }));
  assert.equal(igual.sugerencia_ia.coincide_con_reglas, true);
  const distinto = presentarEvaluacion(fila({ ia: { disponible: true, ...RESPUESTA_OK, version: "v1" } }, "amarillo"));
  assert.equal(distinto.sugerencia_ia.coincide_con_reglas, false);
});

test("la vista explica por que no hay sugerencia de IA", () => {
  assert.deepEqual(presentarEvaluacion(fila({})).sugerencia_ia, { disponible: false, motivo: "no_evaluada" });
  assert.deepEqual(
    presentarEvaluacion(fila({ ia: { disponible: false, motivo: "tiempo_agotado" } })).sugerencia_ia,
    { disponible: false, motivo: "tiempo_agotado" }
  );
});
