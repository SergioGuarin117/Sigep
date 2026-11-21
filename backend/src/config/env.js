require("dotenv").config();

module.exports = {
  PORT: Number(process.env.PORT) || 3000,
  JWT_SECRET: process.env.JWT_SECRET,
  JWT_EXPIRA_EN: "1h",
};
