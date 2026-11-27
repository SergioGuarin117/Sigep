// MIDDLEWARES: validan los registros de historia clinica (morbilidades, medicamentos, vacunas).
const { validarCampos } = require("./validacion");

const MAXIMO_POR_LISTA = 30;

const ESQUEMAS = Object.freeze({
  morbilidades: {
    descripcion: { tipo: "texto", max: 200, requerido: true },
    codigo_cie10: { tipo: "texto", max: 10 },
    desde: { tipo: "fecha", noFutura: true },
  },
  medicamentos: {
    medicamento: { tipo: "texto", max: 160, requerido: true },
    dosis: { tipo: "texto", max: 80 },
    frecuencia: { tipo: "texto", max: 80 },
    requiere_suministro: { tipo: "booleano", noNulo: true },
  },
  vacunas: {
    vacuna: { tipo: "texto", max: 120, requerido: true },
    dosis_numero: { tipo: "entero" },
    fecha_aplicacion: { tipo: "fecha", noFutura: true },
  },
});

// Valida las listas opcionales que pueden venir al registrar un paciente.
// Devuelve { historia } con los datos limpios o { error }.
function validarListas(cuerpo) {
  const historia = {};
  for (const [tipo, esquema] of Object.entries(ESQUEMAS)) {
    if (!(tipo in cuerpo)) continue;
    const lista = cuerpo[tipo];
    if (!Array.isArray(lista) || lista.length > MAXIMO_POR_LISTA) {
      return { error: `${tipo} debe ser una lista de hasta ${MAXIMO_POR_LISTA} elementos` };
    }
    historia[tipo] = [];
    for (const [i, item] of lista.entries()) {
      const { datos, error } = validarCampos(item, esquema);
      if (error) return { error: `${tipo}[${i}]: ${error}` };
      historia[tipo].push(datos);
    }
  }
  return { historia };
}

// Valida el cuerpo de POST /api/pacientes/:id/:tipo (un solo registro).
function validarRegistro(req, res, next) {
  const esquema = ESQUEMAS[req.params.tipo];
  if (!esquema) {
    return res.status(404).json({ error: `Tipo de historia desconocido: ${req.params.tipo}` });
  }
  const { datos, error } = validarCampos(req.body || {}, esquema);
  if (error) return res.status(400).json({ error });
  req.datos = datos;
  next();
}

// Solo acepta los tipos conocidos en la ruta.
function validarTipo(req, res, next) {
  if (!ESQUEMAS[req.params.tipo]) {
    return res.status(404).json({ error: `Tipo de historia desconocido: ${req.params.tipo}` });
  }
  next();
}

module.exports = { ESQUEMAS, validarListas, validarRegistro, validarTipo };
