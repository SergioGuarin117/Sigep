const express = require("express");
const triageController = require("../controllers/triageController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const { validarIdParam } = require("../middlewares/validacion");
const {
  validarConfirmacion,
  validarFiltrosPendientes,
} = require("../middlewares/validarTriage");
const { ROLES } = require("../constants/roles");

const router = express.Router();

router.use(verificarToken);

// Cola de evaluaciones que esperan confirmacion.
router.get(
  "/pendientes",
  permitirRoles(ROLES.MEDICO_TRIAGE, ROLES.ADMIN_ALBERGUE, ROLES.COORDINADOR_MUNICIPAL),
  validarFiltrosPendientes,
  triageController.pendientes
);

// Confirmar el nivel final es una decision medica: solo el medico de triage.
router.patch(
  "/:id/confirmar",
  permitirRoles(ROLES.MEDICO_TRIAGE),
  validarIdParam("id", "El id de la evaluación"),
  validarConfirmacion,
  triageController.confirmar
);

module.exports = router;
