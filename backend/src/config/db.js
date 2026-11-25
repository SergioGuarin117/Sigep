require("dotenv").config();

const { Pool, types } = require("pg");

// BIGINT/BIGSERIAL (ids, COUNT) llegan como texto por defecto; los queremos como numero.
// Es seguro: los ids de este sistema nunca superan 2^53.
types.setTypeParser(types.builtins.INT8, (valor) => Number(valor));

const pool = new Pool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
});

module.exports = pool;
