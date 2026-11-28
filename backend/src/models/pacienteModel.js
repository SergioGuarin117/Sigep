// MODELO: acceso a datos de "pacientes". No conoce HTTP.
const pool = require("../config/db");
const historiaModel = require("./historiaModel");

// Columnas explicitas: la fecha sale como texto AAAA-MM-DD (sin problemas de zona horaria).
const COLUMNAS = `
  p.id, p.numero, p.grupo_familiar_id, p.albergue_id, p.tipo_documento, p.numero_documento,
  p.nombres, p.apellidos,
  TO_CHAR(p.fecha_nacimiento, 'YYYY-MM-DD') AS fecha_nacimiento,
  DATE_PART('year', AGE(p.fecha_nacimiento))::int AS edad,
  p.sexo, p.tipo_sangre, p.telefono, p.gestante, p.condiciones_previas, p.observaciones,
  p.cerrado, p.datos_ocr, p.registrado_por, p.creado_en, p.actualizado_en,
  t.triage_id, t.triage_sugerido, t.triage_final, t.triage_evaluado_en,
  t.triage_sindrome, t.triage_estado_clinico, t.triage_requiere_traslado`;

// Ultima evaluacion de triage de cada paciente (si tiene alguna).
const UNION_TRIAGE = `
  LEFT JOIN LATERAL (
    SELECT e.id AS triage_id, e.nivel_sugerido_ia AS triage_sugerido,
           e.nivel_final AS triage_final, e.evaluado_en AS triage_evaluado_en,
           e.sindrome AS triage_sindrome, e.estado_clinico AS triage_estado_clinico,
           e.requiere_traslado AS triage_requiere_traslado
    FROM evaluaciones_triage e
    WHERE e.paciente_id = p.id
    ORDER BY e.evaluado_en DESC, e.id DESC
    LIMIT 1
  ) t ON TRUE`;

// Campos que se pueden escribir (lista blanca: se usa para armar el SQL).
const COLUMNAS_ESCRIBIBLES = [
  "grupo_familiar_id", "albergue_id", "tipo_documento", "numero_documento", "nombres",
  "apellidos", "fecha_nacimiento", "sexo", "tipo_sangre", "telefono", "gestante",
  "condiciones_previas", "observaciones", "cerrado", "datos_ocr",
];

function escaparLike(texto) {
  return texto.replace(/[\\%_]/g, "\\$&");
}

// datos_ocr es JSONB: se envia como texto JSON.
function valorSql(campo, valor) {
  return campo === "datos_ocr" && valor !== null ? JSON.stringify(valor) : valor;
}

async function crear(datos, usuarioId, cliente = pool) {
  const columnas = COLUMNAS_ESCRIBIBLES.filter((c) => c in datos);
  const valores = columnas.map((c) => valorSql(c, datos[c]));
  columnas.push("registrado_por");
  valores.push(usuarioId);

  const marcadores = columnas.map((_, i) => `$${i + 1}`);
  const resultado = await cliente.query(
    `INSERT INTO pacientes (${columnas.join(", ")}) VALUES (${marcadores.join(", ")}) RETURNING id`,
    valores
  );
  return resultado.rows[0].id;
}

// Registra el paciente y su historia clinica inicial en una sola transaccion.
async function crearCompleto(datos, usuarioId, historia) {
  const cliente = await pool.connect();
  try {
    await cliente.query("BEGIN");
    const id = await crear(datos, usuarioId, cliente);
    await historiaModel.agregarVarios(id, historia, cliente);
    await cliente.query("COMMIT");
    return id;
  } catch (error) {
    await cliente.query("ROLLBACK");
    throw error;
  } finally {
    cliente.release();
  }
}

async function listar({ albergue_id, grupo_familiar_id, cerrado, nivel, texto, limite, desplazamiento }) {
  const condiciones = [];
  const valores = [];
  const agregar = (sql, valor) => {
    valores.push(valor);
    condiciones.push(sql.replace("?", `$${valores.length}`));
  };

  if (albergue_id !== undefined) agregar("p.albergue_id = ?", albergue_id);
  if (grupo_familiar_id !== undefined) agregar("p.grupo_familiar_id = ?", grupo_familiar_id);
  if (cerrado !== undefined) agregar("p.cerrado = ?", cerrado);
  // nivel vigente = el confirmado por el medico o, mientras no lo haya, el sugerido.
  if (nivel !== undefined) agregar("COALESCE(t.triage_final, t.triage_sugerido) = ?", nivel);
  if (texto) {
    valores.push(`%${escaparLike(texto)}%`);
    const n = valores.length;
    condiciones.push(
      `(p.nombres ILIKE $${n} OR p.apellidos ILIKE $${n} OR p.numero_documento ILIKE $${n}
        OR (p.nombres || ' ' || p.apellidos) ILIKE $${n})`
    );
  }
  const where = condiciones.length ? `WHERE ${condiciones.join(" AND ")}` : "";
  valores.push(limite, desplazamiento);

  const resultado = await pool.query(
    `SELECT ${COLUMNAS}, COUNT(*) OVER() AS total
     FROM pacientes p
     ${UNION_TRIAGE}
     ${where}
     ORDER BY p.creado_en DESC, p.id DESC
     LIMIT $${valores.length - 1} OFFSET $${valores.length}`,
    valores
  );
  const total = resultado.rows.length ? Number(resultado.rows[0].total) : 0;
  return { total, pacientes: resultado.rows.map(({ total: _t, ...paciente }) => paciente) };
}

async function buscarPorId(id, cliente = pool) {
  const resultado = await cliente.query(`SELECT ${COLUMNAS} FROM pacientes p ${UNION_TRIAGE} WHERE p.id = $1`, [id]);
  return resultado.rows[0] || null;
}

async function existe(id) {
  const resultado = await pool.query("SELECT 1 FROM pacientes WHERE id = $1", [id]);
  return resultado.rows.length > 0;
}

async function actualizar(id, cambios) {
  const columnas = COLUMNAS_ESCRIBIBLES.filter((c) => c in cambios);
  const asignaciones = columnas.map((c, i) => `${c} = $${i + 1}`);
  const valores = columnas.map((c) => valorSql(c, cambios[c]));
  valores.push(id);

  const resultado = await pool.query(
    `UPDATE pacientes SET ${asignaciones.join(", ")} WHERE id = $${valores.length} RETURNING id`,
    valores
  );
  return resultado.rows[0] ? buscarPorId(id) : null;
}

module.exports = { crear, crearCompleto, listar, buscarPorId, existe, actualizar };
