const express = require("express");
const pool = require("./db");
const authRoutes = require("./authRoutes");

const app = express();
const PORT = 3000;


app.use(express.json());
app.use("/api/auth", authRoutes);

app.get("/", (req, res) => {
  res.json({ mensaje: "Backend de SIGEP activo" });
});

app.get("/api/health/db", async (req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ mensaje: "Conexión con PostgreSQL correcta" });
  } catch (error) {
    console.error("Detalle del error de PostgreSQL:", error);
    res.status(500).json({ error: "No se pudo conectar con la base de datos" });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Servidor SIGEP escuchando en el puerto ${PORT}`);
});