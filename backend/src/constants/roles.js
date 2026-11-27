// Roles del sistema (documento guia). Deben coincidir con el CHECK de
// usuarios.rol en db/init/03-usuarios.sql.
const ROLES = Object.freeze({
  BRIGADISTA: "brigadista",
  MEDICO_TRIAGE: "medico_triage",
  ADMIN_ALBERGUE: "admin_albergue",
  DIRECTOR_IPS: "director_ips",
  COORDINADOR_MUNICIPAL: "coordinador_municipal",
});

const ROLES_VALIDOS = Object.freeze(Object.values(ROLES));

// Roles que trabajan con el censo de pacientes y grupos familiares.
const ROLES_CENSO = Object.freeze([
  ROLES.BRIGADISTA,
  ROLES.MEDICO_TRIAGE,
  ROLES.ADMIN_ALBERGUE,
  ROLES.COORDINADOR_MUNICIPAL,
]);

// El registro publico siempre crea usuarios con el rol de menor privilegio.
// Solo un coordinador municipal podra cambiarlo (rama auth-gestion-usuarios-api).
const ROL_POR_DEFECTO = ROLES.BRIGADISTA;

module.exports = { ROLES, ROLES_VALIDOS, ROLES_CENSO, ROL_POR_DEFECTO };
