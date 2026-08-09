#!/usr/bin/env node
/**
 * Script de migración de datos: Tareas (Room SQLite → PostgreSQL)
 *
 * Uso:
 *   node scripts/migrate_tareas.js ./path/to/tareas_db.sqlite
 *
 * Requisitos:
 *   - DATABASE_URL en el entorno (apuntar a Postgres local para test, Render para prod)
 *   - El .db extraído del móvil con:
 *     adb shell "run-as com.example.mytaskmyhabit cat databases/tareas_db" > tareas_db.sqlite
 *
 * El script es IDEMPOTENTE: ejecutarlo varias veces no duplica datos.
 */

const path = require('path');
const Database = require('better-sqlite3');
const { Pool } = require('pg');

// --- Configuración ---
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const USUARIO_ID = 1; // Usuario único: tony

// --- Validar argumentos ---
const sqlitePath = process.argv[2];
if (!sqlitePath) {
  console.error('Uso: node scripts/migrate_tareas.js <ruta-al-fichero.sqlite>');
  process.exit(1);
}

if (!process.env.DATABASE_URL) {
  console.error('ERROR: DATABASE_URL no está definida en el entorno.');
  process.exit(1);
}

// --- Conexiones ---
const sqlite = new Database(sqlitePath, { readonly: true });
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_URL.includes('localhost')
    ? false
    : { rejectUnauthorized: false }
});

// --- Helpers de conversión ---

/** Epoch millis (SQLite) → Date ISO para TIMESTAMPTZ en Postgres */
function epochMillisToTimestamp(millis) {
  if (millis == null || millis === 0) return null;
  return new Date(millis).toISOString();
}

/** 0/1 (SQLite) → Boolean */
function intToBool(val) {
  return val === 1;
}

/** Valida que la prioridad sea un valor ENUM válido */
function normalizePrioridad(val) {
  const valid = ['ALTA', 'MEDIA', 'BAJA'];
  if (val && valid.includes(val.toUpperCase())) return val.toUpperCase();
  return 'MEDIA'; // fallback seguro
}

/** Normaliza fecha ISO (YYYY-MM-DD) o epoch day (número) a DATE string */
function normalizeDate(val) {
  if (val == null || val === '') return null;
  // Room almacena LocalDate como epoch day (Long) via TypeConverter
  if (typeof val === 'number') {
    const d = new Date(val * 86400000); // epoch day → millis
    return d.toISOString().slice(0, 10);
  }
  // Si ya es string ISO
  if (/^\d{4}-\d{2}-\d{2}$/.test(val)) return val;
  return null;
}

/** Normaliza hora (HH:MM o HH:MM:SS) a TIME string */
function normalizeTime(val) {
  if (val == null || val === '') return null;
  // Room almacena LocalTime como ISO string via TypeConverter
  if (/^\d{2}:\d{2}(:\d{2})?/.test(val)) return val;
  return null;
}

// --- Migración principal ---
async function migrate() {
  const stats = {
    categorias: { sqlite: 0, inserted: 0, skipped: 0 },
    tareas: { sqlite: 0, inserted: 0, skipped: 0, orphanCategory: 0 },
    errors: []
  };

  const client = await pool.connect();

  try {
    // ══════════════════════════════════════════════════════════════
    // PASO 1: Migrar categorías
    // ══════════════════════════════════════════════════════════════
    console.log('\n═══ PASO 1: Migrando categorías ═══\n');

    const categoriasSqlite = sqlite.prepare('SELECT * FROM categorias_table').all();
    stats.categorias.sqlite = categoriasSqlite.length;
    console.log(`  Categorías en SQLite: ${categoriasSqlite.length}`);

    // Mapa titulo → id_postgres (se llena con las ya existentes + las nuevas)
    const categoriaMap = new Map();

    for (const cat of categoriasSqlite) {
      const fechaCreacion = epochMillisToTimestamp(cat.fechaCreacion) || new Date().toISOString();
      const activa = intToBool(cat.activa ?? 1);

      try {
        const { rows } = await client.query(
          `INSERT INTO categorias_table (usuario_id, titulo, icono, fecha_creacion, activa)
           VALUES ($1, $2, $3, $4, $5)
           ON CONFLICT (usuario_id, titulo) DO NOTHING
           RETURNING id`,
          [USUARIO_ID, cat.titulo, cat.icono || 'list', fechaCreacion, activa]
        );

        if (rows.length > 0) {
          categoriaMap.set(cat.titulo, rows[0].id);
          stats.categorias.inserted++;
        } else {
          stats.categorias.skipped++;
          // Obtener el id existente
          const { rows: existing } = await client.query(
            'SELECT id FROM categorias_table WHERE usuario_id = $1 AND titulo = $2',
            [USUARIO_ID, cat.titulo]
          );
          if (existing.length > 0) categoriaMap.set(cat.titulo, existing[0].id);
        }
      } catch (err) {
        stats.errors.push(`Categoría "${cat.titulo}": ${err.message}`);
      }
    }

    console.log(`  Insertadas: ${stats.categorias.inserted}`);
    console.log(`  Saltadas (ya existían): ${stats.categorias.skipped}`);
    console.log(`  Mapa de categorías: ${categoriaMap.size} entradas`);

    // ══════════════════════════════════════════════════════════════
    // PASO 2: Migrar tareas
    // ══════════════════════════════════════════════════════════════
    console.log('\n═══ PASO 2: Migrando tareas ═══\n');

    const tareasSqlite = sqlite.prepare('SELECT * FROM tareas_table').all();
    stats.tareas.sqlite = tareasSqlite.length;
    console.log(`  Tareas en SQLite: ${tareasSqlite.length}`);

    for (const t of tareasSqlite) {
      // Resolver categoria_id
      let categoriaId = null;
      if (t.categoria && t.categoria.trim() !== '' && t.categoria !== 'null') {
        categoriaId = categoriaMap.get(t.categoria) || null;
        if (categoriaId === null) {
          stats.tareas.orphanCategory++;
        }
      }

      const fechaCreacion = epochMillisToTimestamp(t.fechaCreacion) || new Date().toISOString();
      const estaCompletada = intToBool(t.estaCompletada ?? 0);
      const prioridad = normalizePrioridad(t.prioridad);
      const fechaLimite = normalizeDate(t.fechaLimite);
      const horaLimite = normalizeTime(t.horaLimite);
      const pendienteClasificar = intToBool(t.pendienteClasificar ?? 0);
      const repeticionFin = normalizeDate(t.repeticionFin);
      const repeticionVeces = t.repeticionVeces ?? null;
      const repeticionContador = t.repeticionContador ?? 0;

      try {
        const { rows } = await client.query(
          `INSERT INTO tareas_table (
            usuario_id, titulo, descripcion, esta_completada, prioridad,
            fecha_creacion, fecha_limite, hora_limite, categoria_id,
            repeticion, pendiente_clasificar, repeticion_fin,
            repeticion_veces, repeticion_contador
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
          ON CONFLICT (usuario_id, titulo, fecha_creacion) DO NOTHING
          RETURNING id`,
          [
            USUARIO_ID,
            t.titulo,
            t.descripcion || null,
            estaCompletada,
            prioridad,
            fechaCreacion,
            fechaLimite,
            horaLimite,
            categoriaId,
            t.repeticion || 'Sin repetición',
            pendienteClasificar,
            repeticionFin,
            repeticionVeces,
            repeticionContador
          ]
        );

        if (rows.length > 0) {
          stats.tareas.inserted++;
        } else {
          stats.tareas.skipped++;
        }
      } catch (err) {
        stats.errors.push(`Tarea "${t.titulo}": ${err.message}`);
      }
    }

    console.log(`  Insertadas: ${stats.tareas.inserted}`);
    console.log(`  Saltadas (ya existían): ${stats.tareas.skipped}`);
    console.log(`  Con categoría huérfana (→ NULL): ${stats.tareas.orphanCategory}`);

  } finally {
    client.release();
  }

  // ══════════════════════════════════════════════════════════════
  // RESUMEN FINAL
  // ══════════════════════════════════════════════════════════════
  console.log('\n═══════════════════════════════════════════');
  console.log('         RESUMEN DE MIGRACIÓN');
  console.log('═══════════════════════════════════════════');
  console.log(`  Categorías SQLite:       ${stats.categorias.sqlite}`);
  console.log(`  Categorías insertadas:   ${stats.categorias.inserted}`);
  console.log(`  Categorías saltadas:     ${stats.categorias.skipped}`);
  console.log(`  Tareas SQLite:           ${stats.tareas.sqlite}`);
  console.log(`  Tareas insertadas:       ${stats.tareas.inserted}`);
  console.log(`  Tareas saltadas:         ${stats.tareas.skipped}`);
  console.log(`  Tareas cat. huérfana:    ${stats.tareas.orphanCategory}`);
  if (stats.errors.length > 0) {
    console.log(`\n  ⚠️  ERRORES (${stats.errors.length}):`);
    stats.errors.forEach((e) => console.log(`    - ${e}`));
  } else {
    console.log(`\n  ✓ Sin errores`);
  }
  console.log('═══════════════════════════════════════════\n');

  await pool.end();
  sqlite.close();

  process.exit(stats.errors.length > 0 ? 1 : 0);
}

migrate().catch((err) => {
  console.error('ERROR FATAL:', err);
  process.exit(1);
});
