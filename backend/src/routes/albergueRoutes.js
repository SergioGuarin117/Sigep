const express = require("express");
const albergueController = require("../controllers/albergueController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const { validarIdParam } = require("../middlewares/validacion");
const {
  validarFiltros,
  validarCreacion,
  validarCambios,
} = require("../middlewares/validarAlbergues");
const { ROLES } = require("../constants/roles");

const router = express.Router();
const soloCoordinador = permitirRoles(ROLES.COORDINADOR_MUNICIPAL);

router.use(verificarToken);

router.get("/", validarFiltros, albergueController.listar);
router.get("/:id", validarIdParam("id", "El id del albergue"), albergueController.obtener);
router.post("/", soloCoordinador, validarCreacion, albergueController.crear);
router.patch(
  "/:id",
  soloCoordinador,
  validarIdParam("id", "El id del albergue"),
  validarCambios,
  albergueController.actualizar
);

module.exports = router;
