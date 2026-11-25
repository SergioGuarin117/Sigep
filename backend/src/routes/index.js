// Punto unico donde se montan todas las rutas de la API.
const express = require("express");
const authRoutes = require("./authRoutes");
const usuarioRoutes = require("./usuarioRoutes");
const healthRoutes = require("./healthRoutes");

const router = express.Router();

router.use("/api/auth", authRoutes);
router.use("/api/usuarios", usuarioRoutes);
router.use("/", healthRoutes);

module.exports = router;
