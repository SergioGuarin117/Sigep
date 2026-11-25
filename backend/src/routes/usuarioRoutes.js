const express = require("express");
const usuarioController = require("../controllers/usuarioController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const {
  validarIdParam,
  validarFiltros,
  validarCambiosUsuario,
} = require("../middlewares/validarUsuarios");
const { ROLES } = require("../constants/roles");

const router = express.Router();

// Todo este modulo es exclusivo del coordinador municipal.
router.use(verificarToken, permitirRoles(ROLES.COORDINADOR_MUNICIPAL));

router.get("/", validarFiltros, usuarioController.listar);
router.get("/:id", validarIdParam, usuarioController.obtener);
router.patch("/:id", validarIdParam, validarCambiosUsuario, usuarioController.actualizar);

module.exports = router;
