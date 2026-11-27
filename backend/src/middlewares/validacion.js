// Utilidades comunes de validacion para los modulos de la API.
const LIMITE_POR_DEFECTO = 50;
const LIMITE_MAXIMO = 200;

const enteroPositivo = (v) => Number.isInteger(v) && v > 0;
const esObjeto = (v) => v !== null && typeof v === "object" && !Array.isArray(v);

// Middleware que valida un parametro de ruta numerico (por defecto :id).
function validarIdParam(nombre = "id", etiqueta = "El id") {
  return (req, res, next) => {
    const valor = Number(req.params[nombre]);
    if (!enteroPositivo(valor)) {
      return res.status(400).json({ error: `${etiqueta} no es válido` });
    }
    req.params[nombre] = valor;
    next();
  };
}

// Lee limite y desplazamiento de la query. Devuelve { limite, desplazamiento } o { error }.
function leerPaginacion(query) {
  const limite = query.limite === undefined ? LIMITE_POR_DEFECTO : Number(query.limite);
  const desplazamiento = query.desplazamiento === undefined ? 0 : Number(query.desplazamiento);
  if (!enteroPositivo(limite) || limite > LIMITE_MAXIMO) {
    return { error: `limite debe estar entre 1 y ${LIMITE_MAXIMO}` };
  }
  if (!Number.isInteger(desplazamiento) || desplazamiento < 0) {
    return { error: "desplazamiento debe ser 0 o mayor" };
  }
  return { limite, desplazamiento };
}

function leerBooleanoQuery(valor, nombre) {
  if (valor === undefined) return { valor: undefined };
  if (valor !== "true" && valor !== "false") return { error: `${nombre} debe ser true o false` };
  return { valor: valor === "true" };
}

const FECHA = /^\d{4}-\d{2}-\d{2}$/;
function esFecha(texto) {
  if (typeof texto !== "string" || !FECHA.test(texto)) return false;
  const fecha = new Date(`${texto}T00:00:00Z`);
  return !Number.isNaN(fecha.getTime()) && fecha.toISOString().slice(0, 10) === texto;
}

/**
 * Valida un objeto contra un esquema { campo: { tipo, requerido, noNulo, max, valores } }.
 * Tipos: texto, fecha, entero, booleano, enum, objeto, numero (con min/max).
 * Si `parcial` es true ningun campo es obligatorio (PATCH).
 * Devuelve { datos } con solo los campos conocidos, o { error }.
 */
function validarCampos(cuerpo, esquema, { parcial = false } = {}) {
  if (!esObjeto(cuerpo)) return { error: "El cuerpo debe ser un objeto JSON" };

  const desconocidos = Object.keys(cuerpo).filter((c) => !(c in esquema));
  if (desconocidos.length) return { error: `Campos no permitidos: ${desconocidos.join(", ")}` };

  const datos = {};
  for (const [campo, regla] of Object.entries(esquema)) {
    const presente = campo in cuerpo;
    if (!presente) {
      if (regla.requerido && !parcial) return { error: `${campo} es obligatorio` };
      continue;
    }

    let valor = cuerpo[campo];
    if (valor === null || (typeof valor === "string" && valor.trim() === "" && regla.tipo !== "objeto")) {
      if (regla.requerido || regla.noNulo) return { error: `${campo} no puede estar vacío` };
      datos[campo] = null;
      continue;
    }

    switch (regla.tipo) {
      case "texto":
        if (typeof valor !== "string") return { error: `${campo} debe ser texto` };
        valor = valor.trim();
        if (valor.length > (regla.max || 255)) {
          return { error: `${campo} no puede superar ${regla.max || 255} caracteres` };
        }
        break;
      case "fecha":
        if (!esFecha(valor)) return { error: `${campo} debe tener formato AAAA-MM-DD` };
        if (regla.noFutura && valor > new Date().toISOString().slice(0, 10)) {
          return { error: `${campo} no puede ser futura` };
        }
        break;
      case "entero":
        if (!Number.isInteger(valor) || valor < (regla.min ?? 1)) {
          return { error: `${campo} debe ser un entero de ${regla.min ?? 1} o más` };
        }
        break;
      case "numero":
        if (typeof valor !== "number" || !Number.isFinite(valor) || valor < regla.min || valor > regla.max) {
          return { error: `${campo} debe ser un número entre ${regla.min} y ${regla.max}` };
        }
        break;
      case "booleano":
        if (typeof valor !== "boolean") return { error: `${campo} debe ser true o false` };
        break;
      case "enum":
        if (!regla.valores.includes(valor)) {
          return { error: `${campo} debe ser uno de: ${regla.valores.join(", ")}` };
        }
        break;
      case "objeto":
        if (!esObjeto(valor)) return { error: `${campo} debe ser un objeto` };
        break;
      default:
        return { error: `Regla desconocida para ${campo}` };
    }
    datos[campo] = valor;
  }
  return { datos };
}

module.exports = {
  enteroPositivo,
  esObjeto,
  validarIdParam,
  leerPaginacion,
  leerBooleanoQuery,
  validarCampos,
};
