// MODELO: evaluaciones de triage ("evaluaciones_triage"). No conoce HTTP.
const pool = require("../config/db");

const COLUMNAS = `
  e.id, e.paciente_id, e.albergue_id, e.evaluado_por, e.evaluado_en,
  e.frecuencia_cardiaca, e.frecuencia_respiratoria, e.presion_sistolica, e.presion_diastolica,
  e.temperatura::float8 AS temperatura, e.saturacion_oxigeno, e.conciencia,
  e.sintomas, e.sindrome, e.estado_clinico, e.requiere_traslado, e.notas,
  e.nivel_sugerido_ia, e.puntaje_ia::float8 AS puntaje_ia, e.explicacion_ia, e.version_modelo,
  e.nivel_final, e.confirmado_por, e.confirmado_en,
  CONCAT_WS(' ', ue.nombre, ue.apellido) AS evaluado_por_nombre,
  CONCAT_WS(' ', uc.nombre, uc.apellido) AS confirmado_por_nombre`;

const UNIONES = `
  LEFT JOIN usuarios ue ON ue.id = e.evaluado_por
  LEFT JOIN usuarios uc ON uc.id = e.confirmado_por`;

const CAMPOS_ESCRIBIBLES = [
  "frecuencia_cardiaca", "frecuencia_respiratoria", "presion_sistolica", "presion_diastolica",
  "temperatura", "saturacion_oxigeno", "conciencia", "sintomas", "sindrome", "estado_clinico",
  "requiere_traslado", "notas",
];

// datos: campos clinicos validados; sugerencia: resultado de evaluarTriage.
// ia: resultado de iaTriage.predecirTriage (se guarda dentro de explicacion_ia, sin cambiar la tabla).
async function crear({ pacienteId, albergueId, usuarioId, datos, sugerencia, ia = null }) {
  const columnas = CAMPOS_ESCRIBIBLES.filter((c) => c in datos && datos[c] !== null && datos[c] !== undefined);
  const valores = columnas.map((c) => datos[c]);

  columnas.push(
    "paciente_id", "albergue_id", "evaluado_por",
    "nivel_sugerido_ia", "puntaje_ia", "explicacion_ia", "version_modelo"
  );
  valores.push(
    pacienteId, albergueId, usuarioId,
    sugerencia.nivel, sugerencia.puntaje,
    JSON.stringify({
      motivos: sugerencia.motivos,
      datos_faltantes: sugerencia.datos_faltantes,
      confianza: sugerencia.confianza,
      avisos: sugerencia.avisos,
      ia,
    }),
    sugerencia.version
  );

  const marcadores = valores.map((_, i) => `$${i + 1}`);
  const resultado = await pool.query(
    `INSERT INTO evaluaciones_triage (${columnas.join(", ")})
     VALUES (${marcadores.join(", ")}) RETURNING id`,
    valores
  );
  return buscarPorId(resultado.rows[0].id);
}

async function buscarPorId(id) {
  const resultado = await pool.query(
    `SELECT ${COLUMNAS} FROM evaluaciones_triage e ${UNIONES} WHERE e.id = $1`,
    [id]
  );
  return resultado.rows[0] || null;
}

async function listarPorPaciente(pacienteId) {
  const resultado = await pool.query(
    `SELECT ${COLUMNAS} FROM evaluaciones_triage e ${UNIONES}
     WHERE e.paciente_id = $1 ORDER BY e.evaluado_en DESC, e.id DESC`,
    [pacienteId]
  );
  return resultado.rows;
}

// Ultima evaluacion de cada paciente con caso abierto que aun no confirma un medico.
// Orden: rojo, negro, amarillo, verde y, dentro del nivel, la mas antigua primero.
async function listarPendientes({ albergue_id, limite, desplazamiento }) {
  const valores = [];
  let filtroAlbergue = "";
  if (albergue_id !== undefined) {
    valores.push(albergue_id);
    filtroAlbergue = `AND p.albergue_id = $${valores.length}`;
  }
  valores.push(limite, desplazamiento);

  const resultado = await pool.query(
    `SELECT ${COLUMNAS},
            p.numero AS paciente_numero, p.nombres AS paciente_nombres, p.apellidos AS paciente_apellidos,
            p.albergue_id AS paciente_albergue_id,
            DATE_PART('year', AGE(p.fecha_nacimiento))::int AS paciente_edad,
            COUNT(*) OVER() AS total
     FROM (
       SELECT DISTINCT ON (paciente_id) *
       FROM evaluaciones_triage
       ORDER BY paciente_id, evaluado_en DESC, id DESC
     ) e
     JOIN pacientes p ON p.id = e.paciente_id
     ${UNIONES}
     WHERE e.nivel_final IS NULL AND NOT p.cerrado ${filtroAlbergue}
     ORDER BY CASE e.nivel_sugerido_ia
                WHEN 'rojo' THEN 0 WHEN 'negro' THEN 1 WHEN 'amarillo' THEN 2 ELSE 3 END,
              e.evaluado_en, e.id
     LIMIT $${valores.length - 1} OFFSET $${valores.length}`,
    valores
  );
  const total = resultado.rows.length ? Number(resultado.rows[0].total) : 0;
  return { total, pendientes: resultado.rows.map(({ total: _t, ...fila }) => fila) };
}

// Solo confirma si aun no tiene nivel final: asi el historial no se reescribe.
// Devuelve true si confirmo, false si no existia o ya estaba confirmada.
async function confirmar(id, nivelFinal, usuarioId) {
  const resultado = await pool.query(
    `UPDATE evaluaciones_triage
        SET nivel_final = $1, confirmado_por = $2, confirmado_en = NOW()
      WHERE id = $3 AND nivel_final IS NULL
      RETURNING id`,
    [nivelFinal, usuarioId, id]
  );
  return resultado.rows.length > 0;
}

module.exports = { crear, buscarPorId, listarPorPaciente, listarPendientes, confirmar };
