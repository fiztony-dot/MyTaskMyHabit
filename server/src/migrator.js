const fs = require('fs');
const path = require('path');
const { pool } = require('./db');

const MIGRATIONS_DIR = path.join(__dirname, '..', 'migrations');

/**
 * Sistema de migraciones SQL versionadas.
 * - Lee ficheros .sql de server/migrations/ ordenados por nombre (001_xxx.sql, 002_xxx.sql, ...)
 * - Registra las ya aplicadas en la tabla migration_log
 * - Solo ejecuta las pendientes, en orden
 * - Idempotente: ejecutar varias veces no produce errores ni duplicados
 */
async function runMigrations() {
  const client = await pool.connect();
  try {
    // Crear tabla de control si no existe
    await client.query(`
      CREATE TABLE IF NOT EXISTS migration_log (
        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        filename TEXT NOT NULL UNIQUE,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
      )
    `);

    // Leer ficheros de migración ordenados
    const files = fs.readdirSync(MIGRATIONS_DIR)
      .filter((f) => f.endsWith('.sql'))
      .sort();

    if (files.length === 0) {
      console.log('Migraciones: no hay ficheros en migrations/');
      return;
    }

    // Obtener migraciones ya aplicadas
    const { rows: applied } = await client.query('SELECT filename FROM migration_log ORDER BY filename');
    const appliedSet = new Set(applied.map((r) => r.filename));

    // Aplicar las pendientes
    let count = 0;
    for (const file of files) {
      if (appliedSet.has(file)) continue;

      const sql = fs.readFileSync(path.join(MIGRATIONS_DIR, file), 'utf8');
      console.log(`Migraciones: aplicando ${file}...`);

      await client.query('BEGIN');
      try {
        await client.query(sql);
        await client.query('INSERT INTO migration_log (filename) VALUES ($1)', [file]);
        await client.query('COMMIT');
        count++;
        console.log(`Migraciones: ${file} aplicada correctamente`);
      } catch (err) {
        await client.query('ROLLBACK');
        throw new Error(`Error aplicando migración ${file}: ${err.message}`);
      }
    }

    if (count === 0) {
      console.log('Migraciones: todas al día, nada que aplicar');
    } else {
      console.log(`Migraciones: ${count} migración(es) aplicada(s)`);
    }
  } finally {
    client.release();
  }
}

module.exports = { runMigrations };
