// MIDDLEWARES: validan los cuerpos y filtros de las rutas de triage.
const {
  validarCampos,
  leerPaginacion,
  enteroPositivo,
} = require("./validacion");
const { NIVELES } = require("../services/triageReglas");

const MAXIMO_SINTOMAS = 20;

const ESQUEMA_EVALUACION = {
  frecuencia_cardiaca: { tipo: "entero", min: 0, max: 300 },
  frecuencia_respiratoria: { tipo: "entero", min: 0, max: 100 },
  presion_sistolica: { tipo: "entero", min: 0, max: 300 },
  presion_diastolica: { tipo: "entero", min: 0, max: 200 },
  temperatura: { tipo: "numero", min: 25, max: 45 },
  saturacion_oxigeno: { tipo: "entero", min: 0, max: 100 },
  conciencia: { tipo: "enum", valores: ["alerta", "voz", "dolor", "inconsciente"], noNulo: true },
  sindrome: {
    tipo: "enum",
    valores: ["febril", "gastrointestinal", "respiratorio", "trauma", "otro"],
    noNulo: true,
  },
  estado_clinico: { tipo: "enum", valores: ["estable", "observacion", "grave", "critico"], noNulo: true },
  requiere_traslado: { tipo: "booleano", noNulo: true },
  notas: { tipo: "texto", max: 2000 },
};

function validarEvaluacion(req, res, next) {
  const cuerpo = req.body || {};
  // sintomas es una lista de textos cortos: se valida aparte del resto de campos.
  const { sintomas, ...resto } = cuerpo;
  const { datos, error } = validarCampos(resto, ESQUEMA_EVALUACION);
  if (error) return res.status(400).json({ error });

  if (sintomas !== undefined) {
    const valida =
      Array.isArray(sintomas) &&
      sintomas.length <= MAXIMO_SINTOMAS &&
      sintomas.every((s) => typeof s === "string" && s.trim() !== "" && s.length <= 60);
    if (!valida) {
      return res.status(400).json({
        error: `sintomas debe ser una lista de hasta ${MAXIMO_SINTOMAS} textos de máximo 60 caracteres`,
      });
    }
    datos.sintomas = [...new Set(sintomas.map((s) => s.trim()))];
  }

  if (
    datos.presion_sistolica != null &&
    datos.presion_diastolica != null &&
    datos.presion_diastolica > datos.presion_sistolica
  ) {
    return res.status(400).json({ error: "presion_diastolica no puede ser mayor que la sistólica" });
  }

  req.datos = datos;
  next();
}

function validarConfirmacion(req, res, next) {
  const { datos, error } = validarCampos(req.body || {}, {
    nivel_final: { tipo: "enum", valores: NIVELES, requerido: true },
  });
  if (error) return res.status(400).json({ error });
  req.datos = datos;
  next();
}

function validarFiltrosPendientes(req, res, next) {
  const pagina = leerPaginacion(req.query);
  let albergue;
  if (req.query.albergue_id !== undefined) {
    albergue = Number(req.query.albergue_id);
    if (!enteroPositivo(albergue)) {
      return res.status(400).json({ error: "albergue_id debe ser un número entero" });
    }
  }
  if (pagina.error) return res.status(400).json({ error: pagina.error });
  req.filtros = {
    albergue_id: albergue,
    limite: pagina.limite,
    desplazamiento: pagina.desplazamiento,
  };
  next();
}

module.exports = { validarEvaluacion, validarConfirmacion, validarFiltrosPendientes };
