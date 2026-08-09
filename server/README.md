# MyTaskMyHabit — Backend Server

Backend API (Node.js/Express + PostgreSQL) para la app MyTaskMyHabit.

## Estructura

```
server/
├── package.json
├── .env.example
├── README.md
└── src/
    ├── index.js            # Entrada principal, Express setup
    ├── config.js           # Parseo de variables de entorno
    ├── db.js               # Pool PostgreSQL + initSchema
    ├── middleware/
    │   └── auth.js         # Middleware JWT (requireAuth)
    └── routes/
        └── auth.js         # POST /auth/login, GET /auth/me
```

## Requisitos

- Node.js >= 18
- PostgreSQL (local para desarrollo, Render para producción)

## Configuración local

1. **Instalar dependencias:**

   ```bash
   cd server
   npm install
   ```

2. **Crear fichero `.env`** (copiar desde `.env.example`):

   ```bash
   cp .env.example .env
   ```

3. **Configurar variables de entorno en `.env`:**

   | Variable | Descripción |
   |----------|-------------|
   | `PORT` | Puerto del servidor (default: 10000) |
   | `DATABASE_URL` | URL de conexión a PostgreSQL |
   | `JWT_SECRET` | Secreto para firmar tokens JWT |
   | `AUTH_USERS` | Usuarios autorizados (ver formato abajo) |

4. **Generar JWT_SECRET:**

   ```bash
   node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
   ```

5. **Configurar AUTH_USERS:**

   Formato: `usuario1:bcryptHash1,usuario2:bcryptHash2`

   Generar un hash bcrypt para tu contraseña:
   ```bash
   node -e "require('bcryptjs').hash('TU_PASSWORD', 10).then(h => console.log(h))"
   ```

   Luego en `.env`:
   ```
   AUTH_USERS=tony:$2b$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```

6. **Arrancar el servidor en modo desarrollo:**

   ```bash
   npm run dev
   ```

   El servidor arrancará en `http://localhost:10000` con hot-reload (Node --watch).

## Endpoints disponibles

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/health` | No | Health check + estado de conexión a BD |
| POST | `/auth/login` | No | Login → devuelve JWT |
| GET | `/auth/me` | Sí | Datos del usuario autenticado |

### Ejemplo de uso

```bash
# Health check
curl http://localhost:10000/health

# Login
curl -X POST http://localhost:10000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tony","password":"mipass123"}'

# Usar el token recibido
curl http://localhost:10000/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

## Despliegue en Render

### Crear Web Service

1. En [Render Dashboard](https://dashboard.render.com/), crear un nuevo **Web Service**
2. Conectar el repositorio GitHub del monorepo
3. Configurar:
   - **Name:** `mytaskmyhabit-api`
   - **Region:** Frankfurt (EU Central) o la más cercana
   - **Branch:** `main`
   - **Root Directory:** `server`
   - **Runtime:** Node
   - **Build Command:** `npm install`
   - **Start Command:** `npm start`
   - **Plan:** Free (o Starter si necesitas uptime 24/7)

### Crear PostgreSQL

1. En Render, crear una nueva base de datos **PostgreSQL**
2. Nombre sugerido: `mytaskmyhabit-db`
3. Copiar la **Internal Database URL** (si el Web Service está en el mismo grupo) o la **External Database URL**

### Variables de entorno en Render

Configurar en el Web Service:

| Variable | Valor |
|----------|-------|
| `DATABASE_URL` | (la URL de Postgres copiada arriba) |
| `JWT_SECRET` | (generar con el comando de arriba) |
| `AUTH_USERS` | (usuario:hash — ver instrucciones arriba) |

> `PORT` NO se configura manualmente: Render lo inyecta automáticamente.

### Verificar despliegue

```bash
curl https://mytaskmyhabit-api.onrender.com/health
# Debería devolver: {"status":"ok","db":"connected"}
```

## Decisiones de diseño

- **Sin tabla de usuarios en BD:** Los usuarios se definen en la variable de entorno `AUTH_USERS` (mismo patrón que OurMoments). Es una app personal, no hay registro público.
- **Passwords con bcrypt:** A diferencia de OurMoments (que guardaba passwords en plano en la env var), aquí se usan hashes bcrypt (vía `bcryptjs`, implementación pure-JS sin compilación nativa). Más seguro si la variable se expone accidentalmente.
- **JWT sin refresh token:** Expiración de 30 días. Al ser una app personal con uno o dos usuarios, no se justifica la complejidad de un flujo de refresh.
- **initSchema() mínimo:** Solo verifica conexión. Las tablas de negocio (tareas, hábitos, shopping) se crearán en iteraciones posteriores.
- **CORS abierto:** Configurado con `cors()` sin restricciones por ahora. Se restringirá al dominio de la PWA cuando exista.
