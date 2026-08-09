const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const config = require('../config');
const { pool } = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /auth/login
 * Body: { username, password }
 * Valida contra AUTH_USERS (bcrypt hash) y devuelve un JWT (30 días).
 */
router.post('/login', async (req, res, next) => {
  try {
    const { username, password } = req.body || {};

    if (!username || !password) {
      return res.status(400).json({ error: 'Se requieren username y password' });
    }

    const match = config.users.find((u) => u.username === username);
    if (!match) {
      return res.status(401).json({ error: 'Usuario o contraseña incorrectos' });
    }

    const valid = await bcrypt.compare(password, match.passwordHash);
    if (!valid) {
      return res.status(401).json({ error: 'Usuario o contraseña incorrectos' });
    }

    const token = jwt.sign(
      { sub: username },
      config.jwtSecret,
      { expiresIn: '30d' }
    );

    res.json({
      token,
      user: {
        username
      }
    });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /auth/me
 * Protegido por requireAuth — devuelve los datos del usuario autenticado,
 * incluyendo el id numérico de la tabla usuarios (para usar como usuario_id en FKs).
 */
router.get('/me', requireAuth, async (req, res, next) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, username, nombre_visible FROM usuarios WHERE username = $1',
      [req.user.sub]
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario no encontrado en la base de datos' });
    }
    res.json({
      user: {
        id: Number(rows[0].id),
        username: rows[0].username,
        nombreVisible: rows[0].nombre_visible
      }
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
