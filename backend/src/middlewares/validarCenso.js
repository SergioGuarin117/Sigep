// MIDDLEWARES: validan filtros y cuerpos de /api/grupos-familiares y /api/pacientes.
const {
  enteroPositivo,
  esObjeto,
  leerPaginacion,
  leerBooleanoQuery,
  validarCampos,
} = require("./validacion");
const { validarListas } = require("./validarHistoria");

const SEXOS = ["femenino", "masculino", "otro"];
const TIPOS_SANGRE = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const ESQUEMA_GRUPO = {
  albergue_id: { tipo: "entero" },
  zona_afectacion: { tipo: "texto", max: 160 },
  latitud: { tipo: "numero", min: -90, max: 90 },
  longitud: { tipo: "numero", min: -180, max: 180 },
  numero_integrantes: { tipo: "entero", noNulo: true },
  observaciones: { tipo: "texto", max: 2000 },
};

const ESQUEMA_PACIENTE = {
  nombres: { tipo: "texto", max: 100, requerido: true },
  apellidos: { tipo: "texto", max: 100, requerido: true },
  tipo_documento: { tipo: "texto", max: 20 },
  numero_documento: { tipo: "texto", max: 30 },
  fecha_nacimiento: { tipo: "fecha", noFutura: true },
  sexo: { tipo: "enum", valores: SEXOS },
  tipo_sangre: { tipo: "enum", valores: TIPOS_SANGRE },
  telefono: { tipo: "texto", max: 30 },
  gestante: { tipo: "booleano", noNulo: true },
  condiciones_previas: { tipo: "texto", max: 2000 },
  observaciones: { tipo: "texto", max: 2000 },
  grupo_familiar_id: { tipo: "entero" },
  albergue_id: { tipo: "entero" },
  cerrado: { tipo: "booleano", noNulo: true },
  datos_ocr: { tipo: "objeto" },
};

function leerEnteroQuery(valor, nombre) {
  if (valor === undefined) return { valor: undefined };
  const numero = Number(valor);
  if (!enteroPositivo(numero)) return { error: `${nombre} debe ser un número entero` };
  return { valor: numero };
}

function leerTexto(valor) {
  return typeof valor === "string" && valor.trim() ? valor.trim().slice(0, 100) : undefined;
}

function validarFiltrosGrupos(req, res, next) {
  const pagina = leerPaginacion(req.query);
  const albergue = leerEnteroQuery(req.query.albergue_id, "albergue_id");
  const error = pagina.error || albergue.error;
  if (error) return res.status(400).json({ error });
  req.filtros = {
    albergue_id: albergue.valor,
    texto: leerTexto(req.query.q),
    limite: pagina.limite,
    desplazamiento: pagina.desplazamiento,
  };
  next();
}

function validarFiltrosPacientes(req, res, next) {
  const pagina = leerPaginacion(req.query);
  const albergue = leerEnteroQuery(req.query.albergue_id, "albergue_id");
  const grupo = leerEnteroQuery(req.query.grupo_familiar_id, "grupo_familiar_id");
  const cerrado = leerBooleanoQuery(req.query.cerrado, "cerrado");
  const error = pagina.error || albergue.error || grupo.error || cerrado.error;
  if (error) return res.status(400).json({ error });
  req.filtros = {
    albergue_id: albergue.valor,
    grupo_familiar_id: grupo.valor,
    cerrado: cerrado.valor,
    texto: leerTexto(req.query.q),
    limite: pagina.limite,
    desplazamiento: pagina.desplazamiento,
  };
  next();
}

// La ubicacion se indica con latitud y longitud juntas (o ambas null para quitarla).
function errorUbicacion(datos) {
  const hayLat = "latitud" in datos;
  const hayLon = "longitud" in datos;
  if (hayLat !== hayLon) return "latitud y longitud deben enviarse juntas";
  if (hayLat && (datos.latitud === null) !== (datos.longitud === null)) {
    return "latitud y longitud deben ser ambas números o ambas null";
  }
  return null;
}

function validadorGrupo(parcial) {
  return (req, res, next) => {
    const { datos, error } = validarCampos(req.body || {}, ESQUEMA_GRUPO, { parcial });
    const falla = error || (datos && errorUbicacion(datos));
    if (falla) return res.status(400).json({ error: falla });
    if (parcial && Object.keys(datos).length === 0) {
      return res.status(400).json({ error: "Indica al menos un campo a modificar" });
    }
    req.datos = datos;
    next();
  };
}

// El documento se registra completo (tipo y numero) o no se registra.
function errorDocumento(datos, parcial) {
  const tipo = datos.tipo_documento;
  const numero = datos.numero_documento;
  if (parcial) {
    const alguno = "tipo_documento" in datos || "numero_documento" in datos;
    const ambos = "tipo_documento" in datos && "numero_documento" in datos;
    return alguno && !ambos ? "tipo_documento y numero_documento deben enviarse juntos" : null;
  }
  return Boolean(tipo) !== Boolean(numero)
    ? "tipo_documento y numero_documento deben enviarse juntos"
    : null;
}

const LISTAS_HISTORIA = ["morbilidades", "medicamentos", "vacunas"];

function validadorPaciente(parcial) {
  return (req, res, next) => {
    const cuerpo = req.body || {};
    if (!esObjeto(cuerpo)) {
      return res.status(400).json({ error: "El cuerpo debe ser un objeto JSON" });
    }
    if (!parcial && "cerrado" in cuerpo) {
      return res.status(400).json({ error: "Un paciente nuevo no puede crearse cerrado" });
    }

    // Al crear pueden venir las listas de historia clinica; al editar tienen sus propias rutas.
    const campos = { ...cuerpo };
    if (!parcial) LISTAS_HISTORIA.forEach((lista) => delete campos[lista]);

    const { datos, error } = validarCampos(campos, ESQUEMA_PACIENTE, { parcial });
    const listas = error || parcial ? {} : validarListas(cuerpo);
    const falla = error || errorDocumento(datos, parcial) || listas.error;
    if (falla) return res.status(400).json({ error: falla });
    if (parcial && Object.keys(datos).length === 0) {
      return res.status(400).json({ error: "Indica al menos un campo a modificar" });
    }
    if (datos.tipo_documento) datos.tipo_documento = datos.tipo_documento.toUpperCase();
    req.datos = datos;
    req.historia = listas.historia || {};
    next();
  };
}

module.exports = {
  validarFiltrosGrupos,
  validarFiltrosPacientes,
  validarCreacionGrupo: validadorGrupo(false),
  validarCambiosGrupo: validadorGrupo(true),
  validarCreacionPaciente: validadorPaciente(false),
  validarCambiosPaciente: validadorPaciente(true),
};
