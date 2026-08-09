const { Pool } = require('pg');
const config = require('./config');

const pool = new Pool({
  connectionString: config.databaseUrl,
  ssl: config.databaseUrl && config.databaseUrl.includes('localhost')
    ? false
    : { rejectUnauthorized: false }
});

/**
 * Inicializa la BD: verifica conexión y ejecuta migraciones pendientes.
 */
async function initSchema() {
  // Verificar conexión
  const client = await pool.connect();
  try {
    await client.query('SELECT 1');
    console.log('Conexión a PostgreSQL verificada correctamente');
  } finally {
    client.release();
  }

  // Ejecutar migraciones pendientes
  const { runMigrations } = require('./migrator');
  await runMigrations();
}

module.exports = { pool, initSchema };
