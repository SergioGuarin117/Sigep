const app = require("./src/app");
const { PORT } = require("./src/config/env");

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Servidor SIGEP escuchando en el puerto ${PORT}`);
});
