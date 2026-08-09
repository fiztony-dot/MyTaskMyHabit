const { pool } = require('../db');

/**
 * Middleware que resuelve el usuario_id numérico desde el JWT (req.user.sub)
 * y lo adjunta a req.usuarioId. Devuelve 404 si el usuario no existe en la BD.
 * Debe usarse DESPUÉS de requireAuth.
 */
async function resolveUser(req, res, next) {
  try {
    const { rows } = await pool.query(
      'SELECT id FROM usuarios WHERE username = $1',
      [req.user.sub]
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario no encontrado en la base de datos' });
    }
    req.usuarioId = Number(rows[0].id);
    next();
  } catch (err) {
    next(err);
  }
}

module.exports = { resolveUser };
