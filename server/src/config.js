require('dotenv').config();

/**
 * Parsea AUTH_USERS del entorno.
 * Formato: "usuario1:passwordHash1,usuario2:passwordHash2"
 * Donde passwordHash es un hash bcrypt generado previamente.
 */
function parseUsers(raw) {
  return (raw || '')
    .split(',')
    .map((pair) => pair.trim())
    .filter(Boolean)
    .map((pair) => {
      const sepIndex = pair.indexOf(':');
      if (sepIndex === -1) return null;
      const username = pair.slice(0, sepIndex);
      const passwordHash = pair.slice(sepIndex + 1);
      return { username, passwordHash };
    })
    .filter(Boolean);
}

module.exports = {
  port: process.env.PORT || 10000,
  jwtSecret: process.env.JWT_SECRET,
  databaseUrl: process.env.DATABASE_URL,
  users: parseUsers(process.env.AUTH_USERS)
};
