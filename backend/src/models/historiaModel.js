// MODELO: historia clinica basica del paciente (morbilidades, medicamentos cronicos, vacunas).
const pool = require("../config/db");

// Cada tipo de registro y las columnas que se pueden escribir (lista blanca para el SQL).
const TIPOS = Object.freeze({
  morbilidades: {
    tabla: "morbilidades_paciente",
    escribibles: ["descripcion", "codigo_cie10", "desde"],
    salida: "id, descripcion, codigo_cie10, TO_CHAR(desde, 'YYYY-MM-DD') AS desde",
  },
  medicamentos: {
    tabla: "medicamentos_cronicos",
    escribibles: ["medicamento", "dosis", "frecuencia", "requiere_suministro"],
    salida: "id, medicamento, dosis, frecuencia, requiere_suministro",
  },
  vacunas: {
    tabla: "vacunas_paciente",
    escribibles: ["vacuna", "dosis_numero", "fecha_aplicacion"],
    salida: "id, vacuna, dosis_numero, TO_CHAR(fecha_aplicacion, 'YYYY-MM-DD') AS fecha_aplicacion",
  },
});

async function listarTipo(tipo, pacienteId, cliente = pool) {
  const { tabla, salida } = TIPOS[tipo];
  const resultado = await cliente.query(
    `SELECT ${salida} FROM ${tabla} WHERE paciente_id = $1 ORDER BY id`,
    [pacienteId]
  );
  return resultado.rows;
}

async function obtener(pacienteId) {
  const [morbilidades, medicamentos, vacunas] = await Promise.all(
    Object.keys(TIPOS).map((tipo) => listarTipo(tipo, pacienteId))
  );
  return { morbilidades, medicamentos, vacunas };
}

async function agregar(tipo, pacienteId, datos, cliente = pool) {
  const { tabla, escribibles, salida } = TIPOS[tipo];
  const columnas = escribibles.filter((c) => c in datos);
  const valores = [pacienteId, ...columnas.map((c) => datos[c])];
  const marcadores = valores.map((_, i) => `$${i + 1}`);
  const resultado = await cliente.query(
    `INSERT INTO ${tabla} (paciente_id, ${columnas.join(", ")})
     VALUES (${marcadores.join(", ")})
     RETURNING ${salida}`,
    valores
  );
  return resultado.rows[0];
}

// historia: { morbilidades: [...], medicamentos: [...], vacunas: [...] } (cada lista es opcional).
async function agregarVarios(pacienteId, historia, cliente) {
  for (const tipo of Object.keys(TIPOS)) {
    for (const item of historia[tipo] || []) {
      await agregar(tipo, pacienteId, item, cliente);
    }
  }
}

async function eliminar(tipo, pacienteId, itemId) {
  const { tabla } = TIPOS[tipo];
  const resultado = await pool.query(
    `DELETE FROM ${tabla} WHERE id = $1 AND paciente_id = $2 RETURNING id`,
    [itemId, pacienteId]
  );
  return resultado.rows.length > 0;
}

module.exports = { TIPOS, obtener, agregar, agregarVarios, eliminar };
