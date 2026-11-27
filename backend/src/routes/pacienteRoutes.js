const express = require("express");
const pacienteController = require("../controllers/pacienteController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const { validarIdParam } = require("../middlewares/validacion");
const {
  validarFiltrosPacientes,
  validarCreacionPaciente,
  validarCambiosPaciente,
} = require("../middlewares/validarCenso");
const { validarRegistro, validarTipo } = require("../middlewares/validarHistoria");
const { ROLES_CENSO } = require("../constants/roles");

const router = express.Router();
const idPaciente = validarIdParam("id", "El id del paciente");

// Datos de salud de personas: el director de IPS no accede al censo individual.
router.use(verificarToken, permitirRoles(...ROLES_CENSO));

router.get("/", validarFiltrosPacientes, pacienteController.listar);
router.get("/:id", idPaciente, pacienteController.obtener);
router.post("/", validarCreacionPaciente, pacienteController.crear);
router.patch("/:id", idPaciente, validarCambiosPaciente, pacienteController.actualizar);

// Historia clinica: /api/pacientes/:id/(morbilidades|medicamentos|vacunas)
router.post("/:id/:tipo", idPaciente, validarRegistro, pacienteController.agregarHistoria);
router.delete(
  "/:id/:tipo/:itemId",
  idPaciente,
  validarIdParam("itemId", "El id del registro"),
  validarTipo,
  permitirRoles(...pacienteController.ROLES_CIERRE),
  pacienteController.eliminarHistoria
);

module.exports = router;
