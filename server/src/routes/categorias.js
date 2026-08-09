const express = require('express');
const { pool } = require('../db');

const router = express.Router();

/**
 * GET /api/categorias
 * Lista todas las categorías del usuario, ordenadas por fecha_creacion ASC.
 */
router.get('/', async (req, res, next) => {
  try {
    const { rows } = await pool.query(
      `SELECT id, titulo, icono, fecha_creacion, activa
       FROM categorias_table
       WHERE usuario_id = $1
       ORDER BY fecha_creacion ASC`,
      [req.usuarioId]
    );
    res.json({ data: rows });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/categorias
 * Crea una categoría nueva.
 * Body: { titulo, icono? }
 */
router.post('/', async (req, res, next) => {
  try {
    const { titulo, icono } = req.body || {};

    if (!titulo || titulo.trim() === '') {
      return res.status(400).json({ error: 'El campo titulo es obligatorio' });
    }

    const { rows } = await pool.query(
      `INSERT INTO categorias_table (usuario_id, titulo, icono)
       VALUES ($1, $2, $3)
       RETURNING id, titulo, icono, fecha_creacion, activa`,
      [req.usuarioId, titulo.trim(), icono || 'list']
    );

    res.status(201).json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/categorias/:id
 * Edita una categoría existente.
 * Body: { titulo?, icono?, activa? }
 */
router.put('/:id', async (req, res, next) => {
  try {
    const catId = parseInt(req.params.id);
    if (isNaN(catId)) return res.status(400).json({ error: 'ID inválido' });

    // Verificar que pertenece al usuario
    const { rows: existing } = await pool.query(
      'SELECT id FROM categorias_table WHERE id = $1 AND usuario_id = $2',
      [catId, req.usuarioId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Categoría no encontrada' });
    }

    const { titulo, icono, activa } = req.body || {};
    const sets = [];
    const values = [];
    let idx = 1;

    if (titulo !== undefined) { sets.push(`titulo = $${idx++}`); values.push(titulo.trim()); }
    if (icono !== undefined) { sets.push(`icono = $${idx++}`); values.push(icono); }
    if (activa !== undefined) { sets.push(`activa = $${idx++}`); values.push(activa); }

    if (sets.length === 0) {
      return res.status(400).json({ error: 'No se proporcionaron campos para actualizar' });
    }

    values.push(catId, req.usuarioId);
    const { rows } = await pool.query(
      `UPDATE categorias_table SET ${sets.join(', ')}
       WHERE id = $${idx++} AND usuario_id = $${idx}
       RETURNING id, titulo, icono, fecha_creacion, activa`,
      values
    );

    res.json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * DELETE /api/categorias/:id
 * Elimina una categoría. Las tareas asociadas quedan con categoria_id = NULL (FK ON DELETE SET NULL).
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const catId = parseInt(req.params.id);
    if (isNaN(catId)) return res.status(400).json({ error: 'ID inválido' });

    const { rowCount } = await pool.query(
      'DELETE FROM categorias_table WHERE id = $1 AND usuario_id = $2',
      [catId, req.usuarioId]
    );

    if (rowCount === 0) {
      return res.status(404).json({ error: 'Categoría no encontrada' });
    }

    res.json({ data: { deleted: true, id: catId } });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
