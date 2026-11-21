// Punto unico donde se montan todas las rutas de la API.
const express = require("express");
const authRoutes = require("./authRoutes");
const healthRoutes = require("./healthRoutes");

const router = express.Router();

router.use("/api/auth", authRoutes);
router.use("/", healthRoutes);

module.exports = router;
