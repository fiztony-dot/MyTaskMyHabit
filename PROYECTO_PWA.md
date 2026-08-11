# MyTaskMyHabit — Migración a arquitectura cliente-servidor

## Resumen del proyecto

Migración progresiva de la app Android "MyTaskMyHabit" (actualmente 100% local con Room/SQLite) a una arquitectura cliente-servidor: backend Node.js/Express en Render + PostgreSQL en Supabase, autenticación JWT propia, y una PWA futura en Cloudflare Pages. Un solo usuario real. La migración se hace vertical, módulo a módulo: Tareas → Hábitos → Shopping.

## Stack

- **Backend:** Node.js/Express en Render
- **Base de datos:** PostgreSQL en Supabase
- **Auth:** JWT propio, usuarios válidos en variable de entorno AUTH_USERS (bcrypt hashes)
- **App Android:** Kotlin/Jetpack Compose (Room/SQLite, en proceso de retirada módulo a módulo)
- **PWA:** React + Vite, Cloudflare Pages
- **Repositorio:** monorepo en GitHub (proyecto Android + `server/` + `web/`)

## Servicios de terceros

| Servicio | Uso en el proyecto | Plan | Dashboard / acceso | Notas |
|----------|-------------------|------|-------------------|-------|
| Render (Web Service) | Hosting del backend Node/Express | Free | https://dashboard.render.com/ | Nombre del servicio: `mytaskmyhabit-api` |
| Supabase (PostgreSQL) | Base de datos de producción | Free | https://supabase.com/dashboard | Sustituye a Render PostgreSQL (descartado por expiración a 90 días). Sin expiración en el tier free. |
| GitHub | Repositorio monorepo | Free | https://github.com/fiztony-dot/MyTaskMyHabit | Contiene proyecto Android + server/ + web/. Rama: `master` |
| Cloudflare Pages | Hosting de la PWA | Free | https://dash.cloudflare.com/ | Nombre del proyecto: `mytaskmyhabit`. URL: pendiente primer despliegue manual. Root dir: `web`, build command: `npm run build`, output: `dist` |

## Variables de entorno por servicio

| Variable | Dónde vive | Propósito |
|----------|-----------|-----------|
| `DATABASE_URL` | Render (Web Service) — sección Environment | Conexión a PostgreSQL en Supabase (connection string completo) |
| `JWT_SECRET` | Render (Web Service) — sección Environment | Firma de tokens JWT |
| `AUTH_USERS` | Render (Web Service) — sección Environment | Usuarios válidos (formato `usuario:bcryptHash`) |
| `PORT` | Render (Web Service) — inyectado automáticamente | Puerto del servidor (no configurar manualmente) |

## Estado de la migración por módulo

| Módulo | Schema | Migración datos | API | Android | Estado |
|--------|--------|-----------------|-----|---------|--------|
| Base común (auth + usuarios) | ✅ | N/A | ✅ | N/A | Completado (Iteración 1-2) |
| Tareas | ✅ | ✅ | ✅ | ✅ | Completado (Iteraciones 3-7) |
| Hábitos | ⬜ | ⬜ | ⬜ | ⬜ | Pendiente |
| Shopping | ⬜ | ⬜ | ⬜ | ⬜ | Pendiente |
| PWA | ✅ | — | — | — | Scaffolding completado (Iteración P1). Despliegue Cloudflare Pages pendiente primer setup manual. |

## Decisiones de diseño

### Autenticación (Iteración 1)

- **Formato de AUTH_USERS:** `usuario:bcryptHash,usuario2:bcryptHash2`. El separador entre username y hash es el primer `:` (se usa `indexOf(':')` para no romper hashes que contengan `:`). El separador entre usuarios es `,`.
- **JWT payload:** `{ sub: "username" }`, expiración 30 días, sin refresh token (app personal con un solo usuario real).
- **Librería bcrypt:** `bcryptjs` (implementación pure-JS, sin compilación nativa — evita problemas con node-gyp en Windows y CI).
- **CORS:** Abierto (`*`) temporalmente. Pendiente restringir al dominio de la PWA en Cloudflare Pages cuando exista.
- **Sin tabla de usuarios en BD (Iteración 1):** Los usuarios se definen exclusivamente en la variable de entorno AUTH_USERS. No hay registro público ni tabla de contraseñas.

### Tabla usuarios (Iteración 2)

- Tabla `usuarios` creada como referencia de integridad para FKs de todas las tablas de negocio.
- **NO gestiona contraseñas** — eso lo resuelve AUTH_USERS + JWT. Es solo tabla de referencia.
- Schema: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, username TEXT NOT NULL UNIQUE, nombre_visible TEXT, creado_en TIMESTAMPTZ DEFAULT now()`
- Usuario único dado de alta: `tony` (id = 1). El username coincide con el configurado en AUTH_USERS.
- `GET /auth/me` devuelve el id numérico de esta tabla (para usar como `usuario_id` en FKs).

### Migraciones SQL (Iteración 2)

- **Carpeta:** `server/migrations/`
- **Formato de nombres:** `NNN_descripcion.sql` (ej: `001_create_usuarios.sql`, `002_seed_usuario.sql`)
- **Tabla de control:** `migration_log` (campos: `id`, `filename UNIQUE`, `applied_at`)
- **Mecanismo:** Al arrancar el servidor, `initSchema()` llama a `runMigrations()` que:
  1. Crea `migration_log` si no existe (`CREATE TABLE IF NOT EXISTS`)
  2. Lee todos los `.sql` de `migrations/` ordenados por nombre
  3. Compara con los ya registrados en `migration_log`
  4. Aplica los pendientes en orden, cada uno en una transacción (`BEGIN`/`COMMIT`/`ROLLBACK`)
  5. Registra cada migración aplicada con `INSERT INTO migration_log`
- **Idempotencia:** Cada SQL individual usa `IF NOT EXISTS` / `ON CONFLICT DO NOTHING`. Además, el runner nunca reaplica una migración ya registrada.
- **Fichero:** `src/migrator.js`

### Esquema Tareas (Iteración 3)

- **Tipo ENUM:** `prioridad_tarea` con valores `'ALTA'`, `'MEDIA'`, `'BAJA'` (idempotente con `DO $$ ... EXCEPTION WHEN duplicate_object`)
- **categorias_table:** id, usuario_id (FK CASCADE), titulo, icono, fecha_creacion, activa. Índice en usuario_id.
- **tareas_table:** id, usuario_id (FK CASCADE), titulo, descripcion, esta_completada, prioridad (ENUM), fecha_creacion, fecha_limite, hora_limite, categoria_id (FK SET NULL a categorias_table), repeticion, pendiente_clasificar, repeticion_fin, repeticion_veces, repeticion_contador.
- **Corrección respecto a Room:** `categoria_id` es FK formal con `ON DELETE SET NULL` (en Room era un string libre sin integridad referencial). Al borrar una categoría las tareas quedan sin clasificar en vez de huérfanas.
- **Índices:** usuario_id, categoria_id, compuesto (usuario_id, prioridad, fecha_creacion) para la ordenación habitual, parcial (usuario_id, esta_completada WHERE false) para la consulta de pendientes.
- **Nombres de tabla:** Se mantienen `tareas_table` y `categorias_table` (idénticos a Room) para simplificar el mapeo en la migración de datos.

### Migración de datos Tareas (Iteración 4)

- **Script:** `server/scripts/migrate_tareas.js`
- **Dependencia:** `better-sqlite3` (lectura síncrona del .db extraído)
- **Comando de extracción del .db:**
  ```
  adb exec-out "run-as com.example.mistareasapp cat databases/tareas_db" > tareas_db.sqlite
  ```
  (Usar `cmd /c` en Windows para evitar corrupción por encoding de PowerShell)
- **Uso:** `node scripts/migrate_tareas.js ./scripts/tareas_db.sqlite`
- **Idempotencia:** Migración `004_tareas_unique_constraint.sql` crea UNIQUE indexes `(usuario_id, titulo)` en categorías y `(usuario_id, titulo, fecha_creacion)` en tareas. El script usa `ON CONFLICT DO NOTHING`.
- **Resolución de categorías:** El campo `categoria` (string libre en Room) se mapea a `categoria_id` (FK) buscando coincidencia exacta por titulo. Si no existe → NULL.
- **Datos reales verificados (agosto 2026):** 11 categorías, 401 tareas, 67 tareas huérfanas (categorías eliminadas: "MiApp", "App Tareas", "App Hábitos", "App ShopList"), 91 tareas sin categoría, 0 errores de conversión.
- **Tratamiento de "null" string:** Room almacena en algunos casos el literal `"null"` en vez de NULL real; el script lo trata como sin categoría.

### API REST Tareas (Iteración 5)

- **Prefijo:** `/api/` — todos los endpoints protegidos con `requireAuth` + `resolveUser`
- **resolveUser middleware:** Resuelve `req.user.sub` (username del JWT) → `req.usuarioId` (id numérico de tabla usuarios). Se ejecuta después de `requireAuth`.
- **Convención de respuesta:** `{ data: ... }` para éxito, `{ error: "..." }` para errores.

**Endpoints de Categorías:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/categorias` | Lista categorías del usuario (orden: fecha_creacion ASC) |
| POST | `/api/categorias` | Crea categoría. Body: `{ titulo, icono? }` |
| PUT | `/api/categorias/:id` | Edita categoría. Body: `{ titulo?, icono?, activa? }` |
| DELETE | `/api/categorias/:id` | Elimina categoría (tareas quedan con categoria_id=NULL) |

**Endpoints de Tareas:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/tareas` | Lista tareas (orden: prioridad DESC, fecha_creacion ASC). `?pendientes=true` filtra solo no completadas |
| GET | `/api/tareas/:id` | Obtiene una tarea por id |
| POST | `/api/tareas` | Crea tarea. Body: `{ titulo, descripcion?, prioridad?, fecha_limite?, hora_limite?, categoria_id?, repeticion?, pendiente_clasificar?, repeticion_fin?, repeticion_veces? }` |
| PUT | `/api/tareas/:id` | Edita tarea. Body: cualquier subconjunto de campos |
| PATCH | `/api/tareas/:id/completar` | Marca/desmarca completada. Body: `{ esta_completada: bool }` |
| DELETE | `/api/tareas/:id` | Elimina tarea |

### PWA — Scaffolding (Iteración P1)

- **Framework:** React 18 + Vite 6, sin TypeScript (JS es suficiente para app personal con un único desarrollador).
- **Routing:** `react-router-dom` v7 con `BrowserRouter`. Rutas: `/login`, `/tareas` (protegida), `/` → redirect a `/tareas`.
- **Auth en PWA:** Token JWT guardado en `localStorage` (clave `jwt_token`). `ProtectedRoute` comprueba `localStorage` en cada render — si no hay token, redirige a `/login`. Sin contexto React de auth: el token en localStorage es la fuente de verdad, lo cual es suficiente para un solo usuario.
- **Cliente HTTP:** `axios` con interceptor de request (añade `Authorization: Bearer <token>` automáticamente) e interceptor de response (limpia token y redirige a `/login` en 401, **excepto** en el endpoint `/auth/login` propio para evitar redirect en credenciales erróneas).
- **Login response:** El servidor devuelve `{ token, user: { username } }` directamente (sin envoltura `{ data: ... }`). Los endpoints `/api/*` sí usan la convención `{ data: ... }`.
- **PWA manifest:** `vite-plugin-pwa` v0.21 con `registerType: 'autoUpdate'`. Theme color: `#6366f1` (indigo). Iconos: PNG sólidos generados por `scripts/generate-icons.cjs` (sin dependencias externas — solo `zlib` built-in de Node).
- **Service worker strategy:** `generateSW` (Workbox automático). Assets estáticos: precache. Llamadas a la API (`/api/*` en Render): `NetworkFirst` con cache de 24h y máx. 50 entradas.
- **SPA redirect en Cloudflare Pages:** `public/_redirects` con `/* /index.html 200` para que el routing client-side funcione en cualquier URL.
- **`package.json` `"type": "module"`:** Los scripts de utilidad (generate-icons) usan extensión `.cjs` para seguir usando CommonJS sin conflicto con el ES module scope de Vite.
- **Versiones:** Fijadas exactas en `package.json` (sin rangos `^`) siguiendo la convención del proyecto. Resolución desde `package-lock.json`.

### PWA — Auth completo (Iteración P2)

- **Claves localStorage:** `mtmh_token` (JWT) y `mtmh_user` (JSON `{id, username}`). El cambio de claves respecto a P1 (`jwt_token`) implica que los usuarios deban logarse de nuevo tras el deploy — aceptable para app personal.
- **`AuthContext` y patrón de inicialización:**
  1. `useState(() => localStorage.getItem('mtmh_token'))` — inicialización lazy del token para evitar re-renders innecesarios.
  2. `useEffect` en mount llama a `GET /auth/me`. Si el servidor (Render free tier) está dormido y tarda o falla por red (`error.response` es `undefined`), la sesión se restaura desde `mtmh_user` en caché. Solo se limpia la sesión si el servidor devuelve HTTP 401/403 explícitamente.
  3. Después del login: se llama inmediatamente a `GET /auth/me` para enriquecer el usuario básico (del body del login) con el `id` de la tabla usuarios (necesario en P3 para las llamadas a la API).
- **Patrón `setUnauthorizedHandler`:** `client.js` exporta una función para registrar un callback que se llama en respuestas 401 no relacionadas con el login. `AuthContext` registra su `logout()` en un `useEffect`. Esto evita importar el contexto React desde un módulo de red (dependencia circular).
- **`ProtectedRoute` con spinner:** el spinner evita el "auth flash" (login visible brevemente a usuarios autenticados mientras `isLoading = true`). El spinner también aparece en `Login.jsx` por la misma razón — si hay token, se valida antes de mostrar el formulario.
- **Spinner compartido:** `src/components/Spinner.jsx` — CSS animation `@keyframes mtmh-spin` con nombre prefijado para evitar colisiones si se combina CSS global.
- **`useNavigate` en `AuthContext`:** `AuthProvider` debe estar dentro de `BrowserRouter` (que vive en `main.jsx`) para que `useNavigate` funcione. La posición en `App.jsx` (dentro del Router, fuera de Routes) es correcta.

### Despliegue en Cloudflare Pages (setup manual único)

Pasos para conectar el repositorio la primera vez desde https://dash.cloudflare.com/:

1. **Pages → Create project → Connect to Git**
2. Seleccionar repositorio `fiztony-dot/MyTaskMyHabit`
3. Configuración del build:
   - Framework preset: **Vite**
   - Root directory: **`web`**
   - Build command: **`npm run build`**
   - Build output directory: **`dist`**
   - Branch de producción: **`master`**
4. Sin variables de entorno adicionales (la base URL de la API está hardcodeada en `src/api/client.js`)
5. Hacer clic en **Save and Deploy**
6. Una vez desplegado, anotar la URL asignada (formato `*.pages.dev`) y actualizar este fichero

### Adaptación Android (Iteración 6)

- **HTTP Client:** Ktor (ya existía en el proyecto para AI/voz) — no se añadió Retrofit. `ApiClient` singleton con inyección automática de JWT via `defaultRequest`.
- **Token storage:** `AuthManager` usa `DataStore Preferences` (disco interno, cifrado por defecto del sandbox de la app). Persiste `jwt_token` y `username`.
- **Login flow:** `AuthGate` composable envuelve `MisTareasApp()` en `MainActivity`. Si no hay token → muestra `LoginScreen`. Si hay token → carga directa.
- **Arquitectura:** `TareasApiViewModel` es un drop-in replacement del `TareasViewModel` original, con la misma interfaz pública (mismo nombre de propiedades y métodos). La diferencia: en vez de usar Room DAOs con Flows reactivos, hace llamadas HTTP y recarga tras cada mutación.
- **Estrategia de migración:** Room de Tareas se mantiene intacto en el código. El swap se hace en `MiApp.kt` cambiando `TareasViewModel(dao, catDao)` → `TareasApiViewModel()`. Esto permite revertir fácilmente si el corte falla.
- **Mapeo DTO ↔ Modelo:** `TareasApiRepository` traduce entre DTOs del servidor (snake_case, `categoria_id` como FK numérica) y los modelos locales (camelCase, `categoria` como String nombre). Mantiene un mapa `categoriaId → titulo` que se actualiza al cargar.
- **Estados de red:** `TareasApiViewModel` expone `isLoading: StateFlow<Boolean>` y `errorRed: StateFlow<String?>` para que las pantallas muestren feedback.
- **Base URL:** Hardcodeada en `ApiClient.BASE_URL` apuntando al servicio Render. Para desarrollo local se puede cambiar a `http://10.0.2.2:10000`.

## Convenciones establecidas

- Nombres de tabla y columna en **snake_case**, en español (coherencia con el código Android existente)
- Toda tabla de datos lleva `usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE`
- Puerto por defecto del backend: **10000** (coherente con OurMoments)
- Dependencias con versiones fijadas en package.json (no rangos abiertos)
- `trust proxy` activado para funcionar detrás del reverse proxy de Render
- ENUMs de PostgreSQL para campos con valores finitos (ej: `prioridad_tarea`)
- FK formales con `ON DELETE` explícito: `CASCADE` para usuario_id, `SET NULL` para referencias opcionales (ej: categoría de una tarea)
- Índices parciales para consultas frecuentes filtradas (ej: tareas pendientes)
- Nombres de tabla idénticos a Room (`tareas_table`, `categorias_table`) para facilitar mapeo de migración de datos

## Reglas invariables del proyecto

- **NUNCA** romper ni corromper datos existentes en la base de datos de producción. Nunca `DROP`, `TRUNCATE`, `DELETE` masivo sin respaldo verificado previo.
- La app Android debe seguir siendo usable durante toda la migración: el corte de Room a API se hace módulo a módulo, nunca de golpe.
- **Nunca** incluir secretos ni credenciales reales en PROYECTO.md ni en ningún fichero versionado en git — solo referencias a dónde están gestionados.
- Las migraciones SQL deben ser **idempotentes** (`CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ADD COLUMN IF NOT EXISTS`) siempre que sea posible.

## Estructura del monorepo

```
MyTaskMyHabit/
├── app/                    # Proyecto Android (Kotlin/Compose)
├── server/                 # Backend Node.js/Express
│   ├── package.json
│   ├── .env.example
│   ├── README.md
│   ├── migrations/
│   │   ├── 001_create_usuarios.sql
│   │   ├── 002_seed_usuario.sql
│   │   ├── 003_create_tareas_schema.sql
│   │   └── 004_tareas_unique_constraint.sql
│   ├── scripts/
│   │   └── migrate_tareas.js       # Migración datos Room → Postgres
│   └── src/
│       ├── index.js        # Entrada principal, Express setup
│       ├── config.js       # Parseo de variables de entorno
│       ├── db.js           # Pool PostgreSQL + initSchema
│       ├── migrator.js     # Runner de migraciones SQL versionadas
│       ├── middleware/
│       │   ├── auth.js     # Middleware JWT (requireAuth)
│       │   └── resolveUser.js # Resuelve usuario_id desde JWT
│       └── routes/
│           ├── auth.js     # POST /auth/login, GET /auth/me
│           ├── categorias.js # CRUD /api/categorias
│           └── tareas.js   # CRUD /api/tareas
├── web/                    # PWA React + Vite
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   ├── .gitignore
│   ├── scripts/
│   │   └── generate-icons.cjs  # Genera iconos PNG placeholder sin dependencias externas
│   ├── public/
│   │   ├── _redirects          # SPA fallback para Cloudflare Pages
│   │   └── icons/
│   │       ├── icon-192.png
│   │       └── icon-512.png
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       ├── api/
│       │   ├── client.js       # Axios con interceptores de auth y 401
│       │   └── auth.js         # login(), getMe()
│       ├── components/
│       │   ├── ProtectedRoute.jsx  # isLoading→spinner, !auth→/login
│       │   └── Spinner.jsx         # Spinner centrado compartido
│       ├── context/
│       │   └── AuthContext.jsx     # AuthProvider + AuthContext
│       ├── hooks/
│       │   └── useAuth.js          # Hook con error si fuera de AuthProvider
│       └── pages/
│           ├── Login.jsx           # Usa useAuth().login()
│           └── Tareas.jsx          # Placeholder — implementación real en Iteración P3
├── PROYECTO_PWA.md         # Este fichero
├── DATABASE_ANALYSIS.md    # Análisis del schema Room/SQLite actual
└── ...                     # Ficheros del proyecto Android (gradle, etc.)
```

## Historial de iteraciones

| # | Título | Fecha | Resumen |
|---|--------|-------|---------|
| 1 | Esqueleto backend + auth JWT | Agosto 2026 | Backend Node/Express creado en `server/`. Pool PostgreSQL con SSL condicional. Auth JWT con bcryptjs. Endpoints: GET /health, POST /auth/login, GET /auth/me. 10/10 tests passing. Preparado para despliegue en Render (instrucciones en README.md). |
| 2 | Tabla usuarios + migraciones SQL | Agosto 2026 | Sistema de migraciones versionadas (`server/migrations/` + `migrator.js` + tabla `migration_log`). Tabla `usuarios` creada. Usuario único `tony` (id=1) dado de alta con seed idempotente. GET /auth/me actualizado para devolver id numérico. 13/13 tests passing. |
| 3 | Esquema PostgreSQL módulo Tareas | Agosto 2026 | Tipo ENUM `prioridad_tarea`, tabla `categorias_table` y tabla `tareas_table` creadas con FK formales, índices optimizados y corrección del problema de categoría-string-libre de Room. Migración `003_create_tareas_schema.sql`. 12/12 validaciones estáticas OK. Se aplicará en Render en el próximo deploy. |
| 4 | Script migración datos Tareas | Agosto 2026 | Script `scripts/migrate_tareas.js` creado y verificado contra la BD real del dispositivo (11 categorías, 401 tareas, 67 huérfanas de 4 categorías eliminadas, 0 errores de conversión). Migración `004_tareas_unique_constraint.sql` para idempotencia. Comando adb de extracción documentado. NO ejecutado contra producción — pendiente Iteración 7 (corte coordinado). |
| 5 | API REST módulo Tareas | Agosto 2026 | Endpoints CRUD para categorías (`/api/categorias`) y tareas (`/api/tareas`) con auth JWT, resolución de usuario_id desde JWT, validaciones, ordenación por prioridad DESC + fecha_creacion ASC, filtro de pendientes. Middleware `resolveUser` creado. Todos los módulos cargan correctamente. Pendiente desplegar en Render y verificar manualmente. |
| 6 | Adaptar Android: Tareas → API | Agosto 2026 | Capa de red completa con Ktor: `ApiClient` (JWT automático), `AuthManager` (DataStore), `TareasApiService`, `TareasApiRepository` (mapea DTOs ↔ modelos locales). `TareasApiViewModel` como drop-in replacement del ViewModel original. Login screen + AuthGate. Room de Tareas permanece intacto pero desconectado del nuevo flujo. BUILD SUCCESSFUL. Pendiente: swap a TareasApiViewModel en Iteración 7. |
| 7 | Corte coordinado Tareas | Agosto 2026 | Migración de datos ejecutada contra Supabase: 11 categorías insertadas, 401 tareas insertadas, 67 con categoría huérfana (→ NULL), 0 errores. Swap de ViewModel completado (`TareasViewModelRoom` archivado, `TareasViewModel` API activo). App instalada y verificada. Módulo Tareas completamente migrado a servidor. |
| 8 | Limpieza: retirar Room de Tareas | Agosto 2026 | Eliminados: TareaDao, CategoriaDao, TareasViewModelRoom, TareasViewModelFactory, TareaRepository (Room). Tarea.kt y Categoria.kt convertidas a plain data classes (sin @Entity). AppDatabase v31→v32 sin entidades de Tareas. NotificacionWorker y BootReceiver actualizados (no dependen de Room). TareasBackupJson convertido a no-op. Room sigue activo para Hábitos y Shopping. |
| P1 | Scaffolding PWA | Agosto 2026 | Carpeta `web/` creada con React 18 + Vite 6. `vite-plugin-pwa` con manifest (name: MyTaskMyHabit, theme: #6366f1, display: standalone, iconos 192+512). Iconos PNG generados por script propio sin dependencias. Cliente HTTP Axios con interceptores de auth (token de localStorage) y 401 (redirect a /login, excepto en el endpoint de login). Routing protegido con `react-router-dom` v7. Páginas placeholder Login y Tareas. `public/_redirects` para SPA en Cloudflare Pages. Build de producción OK (97 módulos, sw.js + workbox). Verificado en dev: error 401 con credenciales erróneas muestra mensaje correcto. Despliegue Cloudflare Pages: pendiente setup manual (pasos documentados en sección "Decisiones de diseño"). |
| P2 | Auth completo (AuthContext + useAuth + ProtectedRoute robusto) | Agosto 2026 | `AuthContext` con estado de sesión completo (user, token, isLoading, isAuthenticated, login, logout). En mount verifica token con GET /auth/me: 401 cierra sesión, error de red mantiene sesión desde caché localStorage (`mtmh_user`). `useAuth` hook con error descriptivo fuera del provider. `client.js` usa patrón callback `setUnauthorizedHandler` para que el interceptor 401 llame a `logout()` del contexto sin acoplamiento directo. `ProtectedRoute` con spinner durante isLoading. `Login.jsx` usa `useAuth().login()`, muestra spinner durante validación inicial y evita flash del formulario a usuarios autenticados. Clave localStorage: `mtmh_token` / `mtmh_user`. Build OK: 100 módulos. Verificado en dev: routing protegido, redirección correcta. Commit `4b28bbe` pusheado a master. |
