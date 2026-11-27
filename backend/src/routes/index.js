// Punto unico donde se montan todas las rutas de la API.
const express = require("express");
const authRoutes = require("./authRoutes");
const usuarioRoutes = require("./usuarioRoutes");
const albergueRoutes = require("./albergueRoutes");
const grupoFamiliarRoutes = require("./grupoFamiliarRoutes");
const triageRoutes = require("./triageRoutes");
const pacienteRoutes = require("./pacienteRoutes");
const healthRoutes = require("./healthRoutes");

const router = express.Router();

router.use("/api/auth", authRoutes);
router.use("/api/usuarios", usuarioRoutes);
router.use("/api/albergues", albergueRoutes);
router.use("/api/grupos-familiares", grupoFamiliarRoutes);
router.use("/api/pacientes", pacienteRoutes);
router.use("/api/triage", triageRoutes);
router.use("/", healthRoutes);

module.exports = router;
