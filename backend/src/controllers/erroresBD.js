// Traduce errores de PostgreSQL frecuentes a respuestas HTTP claras.
const VIOLACION_UNICA = "23505";
const VIOLACION_FK = "23503";

/**
 * Responde si el error es de unicidad (409) o de referencia inexistente (400).
 * Devuelve true si ya respondio; false si el error debe tratarse como 500.
 */
function responderErrorBD(error, res, { unica, referencia }) {
  if (error.code === VIOLACION_UNICA && unica) {
    res.status(409).json({ error: unica });
    return true;
  }
  if (error.code === VIOLACION_FK && referencia) {
    res.status(400).json({ error: referencia });
    return true;
  }
  return false;
}

module.exports = { responderErrorBD };
