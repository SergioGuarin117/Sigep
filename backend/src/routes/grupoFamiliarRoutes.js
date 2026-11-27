const express = require("express");
const grupoController = require("../controllers/grupoFamiliarController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const { validarIdParam } = require("../middlewares/validacion");
const {
  validarFiltrosGrupos,
  validarCreacionGrupo,
  validarCambiosGrupo,
} = require("../middlewares/validarCenso");
const { ROLES_CENSO } = require("../constants/roles");

const router = express.Router();
const idGrupo = validarIdParam("id", "El id del grupo familiar");

// Datos de salud de personas: el director de IPS no accede al censo individual.
router.use(verificarToken, permitirRoles(...ROLES_CENSO));

router.get("/", validarFiltrosGrupos, grupoController.listar);
router.get("/:id", idGrupo, grupoController.obtener);
router.post("/", validarCreacionGrupo, grupoController.crear);
router.patch("/:id", idGrupo, validarCambiosGrupo, grupoController.actualizar);

module.exports = router;
