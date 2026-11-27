// MIDDLEWARES: validan filtros y cuerpos de las rutas /api/albergues.
const { validarCampos, leerBooleanoQuery } = require("./validacion");

const ESQUEMA_ALBERGUE = {
  nombre: { tipo: "texto", max: 120, requerido: true },
  direccion: { tipo: "texto", max: 255 },
  capacidad_personas: { tipo: "entero", min: 0, noNulo: true },
  latitud: { tipo: "numero", min: -90, max: 90 },
  longitud: { tipo: "numero", min: -180, max: 180 },
  activo: { tipo: "booleano", noNulo: true },
};

function validarFiltros(req, res, next) {
  const { valor: activo, error } = leerBooleanoQuery(req.query.activo, "activo");
  if (error) return res.status(400).json({ error });
  const texto = typeof req.query.q === "string" ? req.query.q.trim().slice(0, 100) : "";
  req.filtros = { activo, texto: texto || undefined };
  next();
}

// La ubicacion se indica con latitud y longitud juntas (o ambas null para quitarla).
function validarUbicacion(datos) {
  const hayLat = "latitud" in datos;
  const hayLon = "longitud" in datos;
  if (hayLat !== hayLon) return "latitud y longitud deben enviarse juntas";
  if (hayLat && (datos.latitud === null) !== (datos.longitud === null)) {
    return "latitud y longitud deben ser ambas números o ambas null";
  }
  return null;
}

function armarValidador(parcial) {
  return (req, res, next) => {
    const { datos, error } = validarCampos(req.body || {}, ESQUEMA_ALBERGUE, { parcial });
    const errorUbicacion = datos && validarUbicacion(datos);
    if (error || errorUbicacion) return res.status(400).json({ error: error || errorUbicacion });
    if (parcial && Object.keys(datos).length === 0) {
      return res.status(400).json({ error: "Indica al menos un campo a modificar" });
    }
    if (!parcial && datos.activo === undefined) delete datos.activo;
    req.datos = datos;
    next();
  };
}

module.exports = {
  validarFiltros,
  validarCreacion: armarValidador(false),
  validarCambios: armarValidador(true),
};
