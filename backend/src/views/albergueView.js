// VISTA: forma en que se presenta un albergue en las respuestas JSON.
function presentarAlbergue(a) {
  const alojadas = Number(a.personas_alojadas);
  return {
    id: a.id,
    nombre: a.nombre,
    direccion: a.direccion,
    latitud: a.latitud,
    longitud: a.longitud,
    capacidad_personas: a.capacidad_personas,
    personas_alojadas: alojadas,
    ocupacion_porcentaje:
      a.capacidad_personas > 0 ? Math.round((alojadas * 100) / a.capacidad_personas) : null,
    activo: a.activo,
    creado_en: a.creado_en,
  };
}

module.exports = { presentarAlbergue };
