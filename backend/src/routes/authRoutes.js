const express = require("express");
const authController = require("../controllers/authController");
const { verificarToken } = require("../middlewares/authMiddleware");
const { validarRegistro, validarLogin } = require("../middlewares/validarAuth");

const router = express.Router();

router.post("/register", validarRegistro, authController.register);
router.post("/login", validarLogin, authController.login);
router.get("/yo", verificarToken, authController.yo);

module.exports = router;
