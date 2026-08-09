const jwt = require('jsonwebtoken');
const config = require('../config');

/**
 * Middleware de autenticación JWT.
 * Lee el header Authorization: Bearer <token>, lo verifica y adjunta
 * el payload decodificado a req.user. Devuelve 401 si falta o es inválido.
 */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: 'Falta el token de autenticación' });
  }

  try {
    req.user = jwt.verify(token, config.jwtSecret);
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Token inválido o expirado' });
  }
}

module.exports = { requireAuth };
