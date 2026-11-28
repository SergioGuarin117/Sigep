require("dotenv").config();

module.exports = {
  PORT: Number(process.env.PORT) || 3000,
  JWT_SECRET: process.env.JWT_SECRET,
  JWT_EXPIRA_EN: "1h",
  // Servicio de IA (FastAPI). Si no responde, el triage sigue funcionando solo con las reglas.
  AI_SERVICE_URL: (process.env.AI_SERVICE_URL || "http://localhost:8000").replace(/\/+$/, ""),
  AI_TIMEOUT_MS: Number(process.env.AI_TIMEOUT_MS) || 3000,
  AI_ACTIVO: process.env.AI_ACTIVO !== "false",
};
