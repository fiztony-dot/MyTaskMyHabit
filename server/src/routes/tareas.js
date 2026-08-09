const express = require('express');
const { pool } = require('../db');

const router = express.Router();

// Mapeo de prioridad para ORDER BY numérico (ALTA=1, MEDIA=2, BAJA=3 → ORDER ASC = ALTA first)
const PRIORIDAD_ORDER = `CASE prioridad WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 END`;

/**
 * GET /api/tareas
 * Lista todas las tareas del usuario.
 * Ordenación: prioridad DESC (ALTA primero), fecha_creacion ASC.
 * Query param: ?pendientes=true → solo esta_completada = false
 */
router.get('/', async (req, res, next) => {
  try {
    const pendientes = req.query.pendientes === 'true';
    let query = `
      SELECT id, titulo, descripcion, esta_completada, prioridad,
             fecha_creacion, fecha_limite, hora_limite, categoria_id,
             repeticion, pendiente_clasificar, repeticion_fin,
             repeticion_veces, repeticion_contador
      FROM tareas_table
      WHERE usuario_id = $1`;

    if (pendientes) {
      query += ` AND esta_completada = false`;
    }

    query += ` ORDER BY ${PRIORIDAD_ORDER} ASC, fecha_creacion ASC`;

    const { rows } = await pool.query(query, [req.usuarioId]);
    res.json({ data: rows });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /api/tareas/:id
 * Obtiene una tarea por id.
 */
router.get('/:id', async (req, res, next) => {
  try {
    const tareaId = parseInt(req.params.id);
    if (isNaN(tareaId)) return res.status(400).json({ error: 'ID inválido' });

    const { rows } = await pool.query(
      `SELECT id, titulo, descripcion, esta_completada, prioridad,
              fecha_creacion, fecha_limite, hora_limite, categoria_id,
              repeticion, pendiente_clasificar, repeticion_fin,
              repeticion_veces, repeticion_contador
       FROM tareas_table
       WHERE id = $1 AND usuario_id = $2`,
      [tareaId, req.usuarioId]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Tarea no encontrada' });
    }

    res.json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/tareas
 * Crea una tarea nueva.
 * Body: { titulo, descripcion?, prioridad?, fecha_limite?, hora_limite?,
 *         categoria_id?, repeticion?, pendiente_clasificar?,
 *         repeticion_fin?, repeticion_veces? }
 */
router.post('/', async (req, res, next) => {
  try {
    const {
      titulo, descripcion, prioridad, fecha_limite, hora_limite,
      categoria_id, repeticion, pendiente_clasificar,
      repeticion_fin, repeticion_veces
    } = req.body || {};

    if (!titulo || titulo.trim() === '') {
      return res.status(400).json({ error: 'El campo titulo es obligatorio' });
    }

    // Validar prioridad si se proporciona
    const validPrioridades = ['ALTA', 'MEDIA', 'BAJA'];
    const priori = prioridad ? prioridad.toUpperCase() : 'MEDIA';
    if (!validPrioridades.includes(priori)) {
      return res.status(400).json({ error: 'Prioridad inválida. Valores: ALTA, MEDIA, BAJA' });
    }

    // Validar que categoria_id pertenece al usuario si se proporciona
    if (categoria_id != null) {
      const { rows: catCheck } = await pool.query(
        'SELECT id FROM categorias_table WHERE id = $1 AND usuario_id = $2',
        [categoria_id, req.usuarioId]
      );
      if (catCheck.length === 0) {
        return res.status(400).json({ error: 'La categoría especificada no existe o no pertenece al usuario' });
      }
    }

    const { rows } = await pool.query(
      `INSERT INTO tareas_table (
        usuario_id, titulo, descripcion, prioridad, fecha_limite, hora_limite,
        categoria_id, repeticion, pendiente_clasificar, repeticion_fin, repeticion_veces
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING id, titulo, descripcion, esta_completada, prioridad,
                fecha_creacion, fecha_limite, hora_limite, categoria_id,
                repeticion, pendiente_clasificar, repeticion_fin,
                repeticion_veces, repeticion_contador`,
      [
        req.usuarioId,
        titulo.trim(),
        descripcion || null,
        priori,
        fecha_limite || null,
        hora_limite || null,
        categoria_id || null,
        repeticion || 'Sin repetición',
        pendiente_clasificar || false,
        repeticion_fin || null,
        repeticion_veces || null
      ]
    );

    res.status(201).json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /api/tareas/:id
 * Edita una tarea existente. Body: cualquier subconjunto de campos.
 */
router.put('/:id', async (req, res, next) => {
  try {
    const tareaId = parseInt(req.params.id);
    if (isNaN(tareaId)) return res.status(400).json({ error: 'ID inválido' });

    // Verificar que pertenece al usuario
    const { rows: existing } = await pool.query(
      'SELECT id FROM tareas_table WHERE id = $1 AND usuario_id = $2',
      [tareaId, req.usuarioId]
    );
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Tarea no encontrada' });
    }

    const allowedFields = [
      'titulo', 'descripcion', 'prioridad', 'fecha_limite', 'hora_limite',
      'categoria_id', 'repeticion', 'pendiente_clasificar', 'esta_completada',
      'repeticion_fin', 'repeticion_veces', 'repeticion_contador'
    ];

    const sets = [];
    const values = [];
    let idx = 1;

    for (const field of allowedFields) {
      if (req.body[field] !== undefined) {
        let val = req.body[field];

        // Validaciones específicas
        if (field === 'prioridad') {
          val = val.toUpperCase();
          if (!['ALTA', 'MEDIA', 'BAJA'].includes(val)) {
            return res.status(400).json({ error: 'Prioridad inválida' });
          }
        }
        if (field === 'categoria_id' && val != null) {
          const { rows: catCheck } = await pool.query(
            'SELECT id FROM categorias_table WHERE id = $1 AND usuario_id = $2',
            [val, req.usuarioId]
          );
          if (catCheck.length === 0) {
            return res.status(400).json({ error: 'Categoría no encontrada' });
          }
        }

        sets.push(`${field} = $${idx++}`);
        values.push(val);
      }
    }

    if (sets.length === 0) {
      return res.status(400).json({ error: 'No se proporcionaron campos para actualizar' });
    }

    values.push(tareaId, req.usuarioId);
    const { rows } = await pool.query(
      `UPDATE tareas_table SET ${sets.join(', ')}
       WHERE id = $${idx++} AND usuario_id = $${idx}
       RETURNING id, titulo, descripcion, esta_completada, prioridad,
                 fecha_creacion, fecha_limite, hora_limite, categoria_id,
                 repeticion, pendiente_clasificar, repeticion_fin,
                 repeticion_veces, repeticion_contador`,
      values
    );

    res.json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * PATCH /api/tareas/:id/completar
 * Marca/desmarca una tarea como completada.
 * Body: { esta_completada: true|false }
 */
router.patch('/:id/completar', async (req, res, next) => {
  try {
    const tareaId = parseInt(req.params.id);
    if (isNaN(tareaId)) return res.status(400).json({ error: 'ID inválido' });

    const { esta_completada } = req.body || {};
    if (typeof esta_completada !== 'boolean') {
      return res.status(400).json({ error: 'El campo esta_completada debe ser boolean' });
    }

    const { rows } = await pool.query(
      `UPDATE tareas_table SET esta_completada = $1
       WHERE id = $2 AND usuario_id = $3
       RETURNING id, titulo, descripcion, esta_completada, prioridad,
                 fecha_creacion, fecha_limite, hora_limite, categoria_id,
                 repeticion, pendiente_clasificar, repeticion_fin,
                 repeticion_veces, repeticion_contador`,
      [esta_completada, tareaId, req.usuarioId]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: 'Tarea no encontrada' });
    }

    res.json({ data: rows[0] });
  } catch (err) {
    next(err);
  }
});

/**
 * DELETE /api/tareas/:id
 * Elimina una tarea.
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const tareaId = parseInt(req.params.id);
    if (isNaN(tareaId)) return res.status(400).json({ error: 'ID inválido' });

    const { rowCount } = await pool.query(
      'DELETE FROM tareas_table WHERE id = $1 AND usuario_id = $2',
      [tareaId, req.usuarioId]
    );

    if (rowCount === 0) {
      return res.status(404).json({ error: 'Tarea no encontrada' });
    }

    res.json({ data: { deleted: true, id: tareaId } });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
