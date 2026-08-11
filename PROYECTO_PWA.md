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
| SendGrid | Email inbound (Inbound Parse) + envío de confirmaciones | Free | https://app.sendgrid.com/ | Inbound Parse: recibe emails y hace POST al webhook. Mail Send: envía confirmaciones al remitente. |

## Variables de entorno por servicio

| Variable | Dónde vive | Propósito |
|----------|-----------|-----------|
| `DATABASE_URL` | Render (Web Service) — sección Environment | Conexión a PostgreSQL en Supabase (connection string completo) |
| `JWT_SECRET` | Render (Web Service) — sección Environment | Firma de tokens JWT |
| `AUTH_USERS` | Render (Web Service) — sección Environment | Usuarios válidos (formato `usuario:bcryptHash`) |
| `PORT` | Render (Web Service) — inyectado automáticamente | Puerto del servidor (no configurar manualmente) |
| `WEBHOOK_SECRET` | Render (Web Service) — sección Environment | Clave para verificar que el webhook viene de SendGrid (incluir en la URL: `?secret=VALOR`) |
| `SENDGRID_API_KEY` | Render (Web Service) — sección Environment | API Key de SendGrid con permiso "Mail Send" para enviar emails de confirmación |
| `SENDGRID_FROM_EMAIL` | Render (Web Service) — sección Environment | Dirección verificada en SendGrid desde la que se envían las confirmaciones |
| `SENDGRID_INBOUND_EMAIL` | Solo documentación | Dirección asignada por SendGrid Inbound Parse a la que el usuario reenvía correos |

## Estado de la migración por módulo

| Módulo | Schema | Migración datos | API | Android | Estado |
|--------|--------|-----------------|-----|---------|--------|
| Base común (auth + usuarios) | ✅ | N/A | ✅ | N/A | Completado (Iteración 1-2) |
| Tareas | ✅ | ✅ | ✅ | ✅ | Completado (Iteraciones 3-7) |
| Hábitos | ⬜ | ⬜ | ⬜ | ⬜ | Pendiente |
| Shopping | ⬜ | ⬜ | ⬜ | ⬜ | Pendiente |
| PWA | ✅ | — | — | — | Scaffolding completado (Iteración P1). Despliegue Cloudflare Pages pendiente primer setup manual. |

## Configuración manual de SendGrid (Iteración E1)

Los siguientes pasos deben realizarse manualmente en el dashboard de SendGrid **una sola vez**:

### a) Crear cuenta y verificar remitente

1. Registrarse en https://sendgrid.com (plan Free: 100 emails/día permanente)
2. Verificar el email remitente: **Settings → Sender Authentication → Single Sender Verification**
   - Este email será `SENDGRID_FROM_EMAIL` (p.ej. `tareas@tudominio.com` o `tu@gmail.com`)
   - SendGrid enviará un email de verificación — hacer clic en el enlace

### b) Generar API Key para envío

1. **Settings → API Keys → Create API Key**
2. Nombre: `mytaskmyhabit-mail`
3. Permisos: **Restricted Access → Mail Send** (solo el permiso necesario)
4. Copiar el API Key generado → guardarlo como `SENDGRID_API_KEY` en Render

### c) Activar Inbound Parse

1. **Settings → Inbound Parse → Add Host & URL**
2. **Subdomain** (opcional): dejar vacío o poner un subdominio propio
3. **Domain**: usar el dominio asignado por SendGrid (si no tienes dominio propio, contactar soporte o usar la dirección que SendGrid proporcione)
4. **Destination URL**:
   ```
   https://mytaskmyhabit-api.onrender.com/webhooks/email?secret=TU_WEBHOOK_SECRET
   ```
   — sustituir `TU_WEBHOOK_SECRET` por el valor generado con `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`
5. **Check spam** y **POST the raw, full MIME message**: dejar **desmarcados** (queremos el parse estructurado con campos individuales)
6. SendGrid asignará una dirección de email inbound (p.ej. `parse@tu-subdominio.sendgrid.net`) — esta es `SENDGRID_INBOUND_EMAIL`

### d) Configurar variables de entorno en Render

En el dashboard de Render, servicio `mytaskmyhabit-api`, sección **Environment**:

| Variable | Valor |
|----------|-------|
| `WEBHOOK_SECRET` | Valor aleatorio generado (hex de 32 bytes) |
| `SENDGRID_API_KEY` | API Key del paso b) |
| `SENDGRID_FROM_EMAIL` | Email verificado del paso a) |

Render redesplegará automáticamente al guardar las variables.

### e) Crear contacto en el gestor de correo

Crear un contacto llamado **"Tareas"** (o similar) con la dirección `SENDGRID_INBOUND_EMAIL` para poder reenviar correos fácilmente desde el móvil.

### f) Verificar el webhook

```bash
WEBHOOK_SECRET=tu_secret node scripts/test_webhook.js https://mytaskmyhabit-api.onrender.com
```

---

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

### PWA — Pantalla Tareas completa (Iteración P3)

- **Carga de datos:** `useTareas` y `useCategorias` se inicializan en parallel desde `Tareas.jsx`. La carga de categorías falla silenciosamente (no bloquea la pantalla; las tareas sin categoría simplemente no muestran etiqueta).
- **Filtro client-side:** Se cargan **todas** las tareas (no solo pendientes). El filtro Todas/Pendientes es estado local en `Tareas.jsx` — no re-fetcha. Esto evita dobles cargas en apps con pocos cientos de ítems (401 en este caso).
- **Toggle optimista:** `useTareas.toggleCompletada` invierte `esta_completada` inmediatamente en el estado local, llama a `PATCH /api/tareas/:id/completar`, y en caso de error revierte al valor original del servidor (`error.response?.data?.data || { ...tarea, esta_completada: !nuevoValor }`). La UX es fluida incluso en conexiones lentas.
- **Grupos por prioridad:** Las tareas pendientes se muestran en secciones ALTA → MEDIA → BAJA. Las secciones vacías no se renderizan. Las completadas van en una sección colapsada al final (oculta por defecto).
- **Estrategia CSS:** Un único fichero `src/styles/tareas.css` importado donde se usa. Prefijo `.t-` para la página de tareas y `.m-` para el modal, para evitar colisiones con estilos globales de `index.css`. Sin CSS-in-JS ni CSS modules — suficiente para una app personal de escala pequeña.
- **Responsive:** `@media (max-width: 480px)` colapsa el grid de 2 columnas del formulario (`.m-row`) a 1 columna, y permite que el título de la tarea haga wrap en vez de truncar con ellipsis.
- **`TareaForm` — hora_limite:** El campo hora está desactivado si no hay fecha límite. Al enviar, si no hay fecha, `hora_limite` se manda como `null` al servidor aunque el campo tenga valor en el estado del formulario.
- **`TareaItem` — fechas:** `isVencida` usa `T12:00:00` (mediodía local) para el parsing, evitando que fechas como `2026-08-11` se interpreten como UTC medianoche y aparezcan vencidas un día antes en zonas horarias +0X.
- **`catMap`:** `Object.fromEntries(categorias.map(c => [c.id, c.titulo]))` — lookup O(1) en `TareaItem` sin propagar el array completo.
- **Build P3:** 107 módulos, `dist/assets/index-Bzu28zq_.js` 245.51 kB (69.49 kB gzip), service worker actualizado.

### PWA — Pantalla Categorías (Iteración P4)

- **`useCategorias` con CRUD completo:** El hook se reescribió para exponer `crear`, `editar`, `eliminar` además de la lista. Ya no filtra por `activa` en el fetch — devuelve todas las categorías. Esto permite a `Categorias.jsx` mostrar categorías inactivas y a `TareaItem.jsx` mostrar el nombre de la categoría aunque esté inactiva. El único lugar donde se filtra es el selector de `TareaForm` (`categorias.filter(c => c.activa !== false)`), para no ofrecer categorías inactivas al crear o editar tareas.
- **Contador de tareas sin endpoint extra:** `Categorias.jsx` llama a `useTareas()` (fetch independiente) y calcula un mapa `categoriaId → nº de tareas` con `reduce`. No se añadió ningún endpoint al backend. El coste es un fetch adicional de las tareas al entrar en `/categorias`, aceptable para 401 registros.
- **Icono como texto libre:** El campo `icono` es un `<input type="text">` (igual que en la app Android). Se muestra en `CategoriaItem` en un badge monospace de color indigo, o con un guión gris si no hay icono. No hay picker visual.
- **Advertencia de eliminación:** `window.confirm` con texto explícito: _"Las tareas asociadas NO se borrarán, pero quedarán sin categoría asignada."_ Refleja el comportamiento real del endpoint `DELETE /api/categorias/:id` (FK `ON DELETE SET NULL` en la base de datos).
- **Checkbox `activa` solo en edición:** El campo activa no existe al crear (el servidor lo inicializa a `true`). Al editar, si se desmarca, aparece un hint: _"Las categorías inactivas no aparecen en el selector al crear tareas."_
- **Prefijo CSS `.c-` / `.cf-`:** Equivalente al `.t-` / `.m-` de `tareas.css`. Cada pantalla tiene su propio fichero CSS (`categorias.css`) sin dependencias entre ellos — los estilos de modal se duplican deliberadamente para evitar acoplamiento.
- **Navegación:** Botón "Categorías" en la cabecera de Tareas (outline indigo, clase `.t-btn-cats`). Botón "←" en la cabecera de Categorías vuelve a `/tareas` con `useNavigate`.
- **Build P4:** 111 módulos, `dist/assets/index-CqDuT9w-.js` 251.14 kB (83.86 kB gzip), service worker actualizado.

### PWA — Rediseño visual y funcional de Tareas (Iteración P3b)

- **Material Icons:** Cargado desde CDN de Google Fonts en `index.html`. Los nombres de icono coinciden con los almacenados en la columna `icono` de `categorias_table` (ej. `work`, `home`, `payments`). Renderizado con `<span class="material-icons">nombre</span>`. No hay fallback local — la app requiere conexión para los iconos (aceptable en PWA con NetworkFirst strategy).
- **Layout pantalla completa:** `max-width: 900px` (antes 700px), background `#f8f9fa` en la página, cards blancas con sombra sutil. En móvil: `max-width: 100%` y secciones sin margen lateral.
- **Header indigo fijo (56px):** 5 iconos Material a la derecha: `search` (buscador), `filter_list` (chips), `visibility`/`visibility_off` (completadas), `category` (navega a /categorias), `exit_to_app` (logout). Los iconos activos tienen fondo blanco semitransparente.
- **Tabs integrados en el header indigo** (fondo `#4f46e5`): "Por vencimiento" y "Por categoría". Pestaña activa con `border-bottom: 2px solid white`.
- **Vista Vencimiento — 7 secciones colapsables:**
  - Clasificación usa fechas locales (`new Date(y, m-1, d)`) para evitar desfase UTC
  - `pendiente_clasificar = true` → siempre va a "Pendientes de clasificar" independientemente de la fecha
  - "Esta semana" = mañana hasta el domingo del calendario actual (incluyendo el domingo)
  - "Este mes" = semana siguiente hasta el último día del mes
  - "Pendientes de clasificar" y "Vencidas" arrancan expandidas; el resto colapsado
  - Cada sección tiene un color y un icono Material propio
- **Vista Categorías:** Secciones colapsables (todas colapsadas por defecto) agrupadas y ordenadas alfabéticamente. Las tareas sin categoría van al final en "Sin categoría" con icono `label_off`.
- **Buscador local:** Filtra por `titulo.includes(texto)` sobre las tareas ya cargadas. Al abrir el buscador, el input recibe autoFocus. Al cerrarlo, borra el texto.
- **Chips de categoría:** Solo muestra categorías activas. El chip seleccionado filtra las dos vistas simultáneamente. Al cerrar el filtro, limpia la selección.
- **TareaItem rediseñado:** Sin botones de editar/eliminar en la lista (tap = abre el form). Icono de categoría (`cat.icono`) a la derecha. Indicador `repeat` si la tarea tiene repetición. Borde izquierdo: rojo (ALTA), ámbar (MEDIA), verde muy pálido (BAJA).
- **FAB:** Botón circular indigo fijo en la esquina inferior derecha. En desktop se posiciona respecto al `max-width: 900px` del contenedor.
- **`catData`:** Añadido a `useCategorias` como `id → objeto completo`, para que `TareaItem` pueda acceder tanto a `titulo` como a `icono` de la categoría en una sola consulta O(1).
- **Build P3b:** 116 módulos, `dist/assets/index-CbaqY0rt.js` 256.64 kB (85.45 kB gzip).

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
│       │   ├── client.js           # Axios con interceptores de auth y 401
│       │   ├── auth.js             # login(), getMe()
│       │   ├── tareas.js           # getTareas, crearTarea, editarTarea, completarTarea, eliminarTarea
│       │   └── categorias.js       # getCategorias
│       ├── components/
│       │   ├── ProtectedRoute.jsx  # isLoading→spinner, !auth→/login
│       │   ├── Spinner.jsx         # Spinner centrado compartido
│       │   ├── CategoriaItem.jsx   # Fila de categoría con icono, contador tareas, acciones
│       │   ├── CategoriaForm.jsx   # Modal crear/editar categoría
│       │   └── tareas/             # Componentes exclusivos de la pantalla de Tareas
│       │       ├── TareaItem.jsx       # Fila con Material Icon de categoría e indicador repetición
│       │       ├── TareaForm.jsx       # Modal crear/editar (campos completos)
│       │       ├── CabeceraTareas.jsx  # Header indigo fijo: título, búsqueda, filtro, logout
│       │       ├── BuscadorTareas.jsx  # Input de búsqueda por título (filtrado local)
│       │       ├── FiltrosCategorias.jsx # Chips horizontales scrollables por categoría
│       │       ├── SeccionVencimiento.jsx # Sección colapsable con color/icono propio
│       │       └── SeccionCategoria.jsx   # Sección colapsable por categoría
│       ├── context/
│       │   └── AuthContext.jsx     # AuthProvider + AuthContext
│       ├── hooks/
│       │   ├── useAuth.js          # Hook con error si fuera de AuthProvider
│       │   ├── useTareas.js        # Estado de lista, CRUD, toggle optimista
│       │   └── useCategorias.js    # CRUD + catMap + catData (id → objeto completo)
│       ├── styles/
│       │   ├── tareas.css          # Estilos pantalla Tareas: layout, header, secciones, FAB, modal
│       │   └── categorias.css      # Estilos pantalla Categorías (prefijos .c- y .cf-)
│       └── pages/
│           ├── Login.jsx           # Usa useAuth().login()
│           ├── Tareas.jsx          # Pantalla principal rediseñada (P3b)
│           └── Categorias.jsx      # Pantalla de gestión de categorías (CRUD completo)
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
| P3 | Pantalla Tareas completa (CRUD, filtros, grupos por prioridad) | Agosto 2026 | Módulos añadidos: `api/tareas.js`, `api/categorias.js`, `hooks/useTareas.js` (toggle optimista con revert automático), `hooks/useCategorias.js` (catMap id→titulo), `components/TareaItem.jsx` (borde de color por prioridad, badge categoría, fecha roja si vencida, badge "Sin clasificar"), `components/TareaForm.jsx` (modal crear/editar con todos los campos, cierre al pulsar fuera). `Tareas.jsx` reescrita: filtro Todas/Pendientes client-side, grupos ALTA/MEDIA/BAJA con sección de completadas colapsable. CSS en `styles/tareas.css` (prefijos `.t-`/`.m-`), responsive en 480 px. Build: 107 módulos, 245.51 kB. Commit `19256bf` pusheado a master. Cloudflare Pages desplegando automáticamente. |
| P4 | Pantalla de Categorías (CRUD completo) | Agosto 2026 | `api/categorias.js` extendido con `crearCategoria`, `editarCategoria`, `eliminarCategoria`. `useCategorias` reescrito con CRUD y catMap que incluye categorías inactivas. `CategoriaItem.jsx` (icono monospace, contador de tareas calculado desde `useTareas`, badge "Inactiva", confirm con advertencia de SET NULL antes de eliminar). `CategoriaForm.jsx` (modal crear/editar, checkbox activa solo en edición, hint explicativo). `Categorias.jsx` (pantalla `/categorias` protegida, back button a Tareas). `TareaForm.jsx` actualizado: selector filtra solo categorías activas. Header de Tareas: botón "Categorías" → navega a `/categorias`. `categorias.css` con prefijos `.c-`/`.cf-`. Build: 111 módulos, 251.14 kB. Commit `2adaf0f` pusheado a master. |
| P3b | Rediseño visual y funcional completo de Tareas | Agosto 2026 | Material Icons desde CDN Google Fonts. Header indigo fijo con 5 iconos (búsqueda, filtro, visibilidad, categorías, logout). Tabs "Por vencimiento" / "Por categoría". 7 secciones colapsables de vencimiento con color e icono Material propio (Pendientes/Vencidas expandidas, resto colapsadas). Vista Categorías agrupada alfabéticamente. Buscador en tiempo real por título (filtrado local). Chips horizontales de filtro por categoría. Toggle mostrar/ocultar completadas. FAB indigo fijo para nueva tarea. `TareaItem` rediseñado sin botones visibles: tap = abre form, icono Material de categoría a la derecha, indicador `repeat`. Layout hasta 900px. `catData` (id→objeto completo) añadido a `useCategorias`. Eliminados `components/TareaItem.jsx` y `components/TareaForm.jsx` (movidos a `components/tareas/`). Build: 116 módulos, 256.64 kB. Commit `d907e1f` pusheado a master. |
| E1 | Email inbound: crear tareas desde correo (SendGrid + backend) | Agosto 2026 | Endpoint `POST /webhooks/email` sin JWT, verificado por `WEBHOOK_SECRET` en query param (403 si no coincide). Parsing multipart/form-data con `multer` (upload.none()). `limpiarCuerpo()` elimina historial quoted (líneas `>`, bloques "On...wrote:", "El...escribió:"), firmas (`--`/`—`/`---`) y trunca a 1000 chars. INSERT en `tareas_table` con `pendiente_clasificar=true`, `prioridad=MEDIA`, `categoria_id=NULL`. Email de confirmación via `@sendgrid/mail` ("✅ Tarea creada: [título]"); si falla se loguea sin romper el webhook. Responde 200 siempre (incluso en error) para que SendGrid no reintente. `server/scripts/test_webhook.js`: 4 tests con `fetch`+`FormData` de Node 18 (403 bad secret, tarea normal, quoted history, sin asunto). Variables de entorno: `WEBHOOK_SECRET`, `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `SENDGRID_INBOUND_EMAIL`. Pasos manuales de configuración SendGrid documentados en PROYECTO_PWA.md. Commit `6f28667` pusheado a master. Deploy Render en curso. |
| P5 | Ajustes finales PWA: expandir/contraer, fade-out, CORS, meta tags | Agosto 2026 | **Botón expandir/contraer todas las secciones** añadido al header (icono `unfold_more`/`unfold_less`): `expandirCtrl` en `Tareas.jsx` (objeto `{open,seq}` que cambia referencia en cada click); `SeccionVencimiento` y `SeccionCategoria` escuchan el prop con `useEffect([expandirCtrl])` para sincronizar estado interno. **Fade-out al completar tarea** con completadas ocultas: `fadingIds` (Set) mantiene la tarea visible 400ms con clase `ti-fading-out` (animación CSS fadeOut+translateX) antes de que el filtro la retire. **Mensaje vacío con término** de búsqueda: "Sin resultados para '{término}'" + "Prueba con otras palabras". **Meta tags Apple PWA**: `apple-mobile-web-app-capable`, `apple-mobile-web-app-status-bar-style`, `apple-mobile-web-app-title`, `apple-touch-icon`. **Manifest PWA**: ya tenía `background_color: '#ffffff'` y `start_url: '/'` correctos. **CORS backend** restringido a `mytaskmyhabitpwa.pages.dev` + `localhost:5173/5174` (eliminado el wildcard `*` del TODO). Build: 116 módulos, 257.75 kB. Commit `f57bfb7` pusheado a master. |
