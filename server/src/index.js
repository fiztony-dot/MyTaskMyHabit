const express = require('express');
const cors = require('cors');
const config = require('./config');
const { pool, initSchema } = require('./db');
const { requireAuth } = require('./middleware/auth');
const { resolveUser } = require('./middleware/resolveUser');

const authRoutes = require('./routes/auth');
const categoriasRoutes = require('./routes/categorias');
const tareasRoutes = require('./routes/tareas');

const app = express();

// Render (y otros PaaS) colocan un reverse proxy delante del servicio
app.set('trust proxy', 1);

const ALLOWED_ORIGINS = [
  'https://mytaskmyhabitpwa.pages.dev',
  'http://localhost:5173',
  'http://localhost:5174',
]
app.use(cors({
  origin: (origin, callback) => {
    if (!origin || ALLOWED_ORIGINS.includes(origin)) callback(null, true)
    else callback(new Error('CORS: origin not allowed'))
  },
  credentials: true,
}));

app.use(express.json());

// --- Endpoint de salud ---
app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', db: 'connected' });
  } catch (err) {
    res.status(503).json({ status: 'error', db: 'disconnected', detail: err.message });
  }
});

// --- Rutas ---
app.use('/auth', authRoutes);
app.use('/api/categorias', requireAuth, resolveUser, categoriasRoutes);
app.use('/api/tareas', requireAuth, resolveUser, tareasRoutes);

// --- Error handler global ---
app.use((err, req, res, next) => {
  console.error(err);
  if (res.headersSent) {
    res.destroy(err);
    return;
  }
  res.status(500).json({ error: err.message || 'Error interno del servidor' });
});

// --- Arranque ---
initSchema()
  .then(() => {
    app.listen(config.port, () => {
      console.log(`MyTaskMyHabit backend listening on port ${config.port}`);
    });
  })
  .catch((err) => {
    console.error('No se pudo conectar a la base de datos', err);
    process.exit(1);
  });
