// MIDDLEWARES: validan parametros y cuerpos de las rutas /api/usuarios.
const { ROLES_VALIDOS } = require("../constants/roles");

const LIMITE_POR_DEFECTO = 50;
const LIMITE_MAXIMO = 200;

const enteroPositivo = (v) => Number.isInteger(v) && v > 0;

function validarIdParam(req, res, next) {
  const id = Number(req.params.id);
  if (!enteroPositivo(id)) {
    return res.status(400).json({ error: "El id del usuario no es válido" });
  }
  req.params.id = id;
  next();
}

function validarFiltros(req, res, next) {
  const { rol, activo, q, limite, desplazamiento } = req.query;
  const filtros = {};

  if (rol !== undefined) {
    if (!ROLES_VALIDOS.includes(rol)) {
      return res.status(400).json({ error: "El rol indicado no existe" });
    }
    filtros.rol = rol;
  }

  if (activo !== undefined) {
    if (activo !== "true" && activo !== "false") {
      return res.status(400).json({ error: "activo debe ser true o false" });
    }
    filtros.activo = activo === "true";
  }

  if (typeof q === "string" && q.trim()) {
    filtros.texto = q.trim().slice(0, 100);
  }

  const lim = limite === undefined ? LIMITE_POR_DEFECTO : Number(limite);
  const des = desplazamiento === undefined ? 0 : Number(desplazamiento);
  if (!enteroPositivo(lim) || lim > LIMITE_MAXIMO) {
    return res.status(400).json({ error: `limite debe estar entre 1 y ${LIMITE_MAXIMO}` });
  }
  if (!Number.isInteger(des) || des < 0) {
    return res.status(400).json({ error: "desplazamiento debe ser 0 o mayor" });
  }
  filtros.limite = lim;
  filtros.desplazamiento = des;

  req.filtros = filtros;
  next();
}

// Cuerpo de PATCH: cualquier subconjunto de rol, albergue_id, ips_id, activo.
function validarCambiosUsuario(req, res, next) {
  const cuerpo = req.body || {};
  const permitidos = ["rol", "albergue_id", "ips_id", "activo"];
  const claves = Object.keys(cuerpo);

  if (claves.length === 0) {
    return res.status(400).json({ error: "Indica al menos un campo a modificar" });
  }
  const desconocidos = claves.filter((c) => !permitidos.includes(c));
  if (desconocidos.length) {
    return res.status(400).json({ error: `Campos no modificables: ${desconocidos.join(", ")}` });
  }

  if ("rol" in cuerpo && !ROLES_VALIDOS.includes(cuerpo.rol)) {
    return res.status(400).json({ error: "El rol indicado no existe" });
  }
  if ("activo" in cuerpo && typeof cuerpo.activo !== "boolean") {
    return res.status(400).json({ error: "activo debe ser true o false" });
  }
  for (const campo of ["albergue_id", "ips_id"]) {
    if (campo in cuerpo && cuerpo[campo] !== null && !enteroPositivo(cuerpo[campo])) {
      return res.status(400).json({ error: `${campo} debe ser un número o null` });
    }
  }

  req.cambios = cuerpo;
  next();
}

module.exports = { validarIdParam, validarFiltros, validarCambiosUsuario };
