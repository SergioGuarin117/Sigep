const express = require("express");
const healthController = require("../controllers/healthController");

const router = express.Router();

router.get("/", healthController.raiz);
router.get("/api/health/db", healthController.baseDeDatos);

module.exports = router;
