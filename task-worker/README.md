# mytaskmyhabit-worker

Cloudflare Worker que actúa como backend HTTP para MyTaskMyHabit.  
Reemplaza el servidor Node/Express alojado en Railway.

**URL de producción:** `https://mytaskmyhabit-worker.fiztony.workers.dev`

## Estructura

```
task-worker/
├── wrangler.toml       # Configuración del Worker (vars + comentarios de secrets)
├── package.json
└── src/
    └── index.js        # Router ES-modules + helpers compartidos + todos los handlers implementados
```

## Comandos

```bash
npm install
npm run dev      # wrangler dev — servidor local en http://localhost:8787
npm run deploy   # wrangler deploy — publicar en Cloudflare
```

## Variables de entorno

### Vars (wrangler.toml `[vars]`)

| Variable            | Descripción                              |
|---------------------|------------------------------------------|
| `SUPABASE_URL`      | URL base del proyecto Supabase           |
| `SENDGRID_FROM_EMAIL` | Dirección de envío (opcional)          |

### Secrets (`npx wrangler secret put <NAME>`)

| Secret                    | Descripción                                              |
|---------------------------|----------------------------------------------------------|
| `JWT_SECRET`              | Clave HMAC-SHA256 para firmar/verificar tokens JWT       |
| `AUTH_USERS`              | JSON con usuarios y hashes de contraseña                 |
| `SUPABASE_SERVICE_ROLE_KEY` | Service-role key de Supabase (bypasa RLS)              |
| `WEBHOOK_SECRET`          | Token de autenticación del email webhook                 |
| `SENDGRID_API_KEY`        | API key de SendGrid (opcional)                           |

## Endpoints

| Método | Ruta                          | Handler                  | Auth |
|--------|-------------------------------|--------------------------|------|
| GET    | `/health`                     | healthCheck              | No   |
| POST   | `/auth/login`                 | handleLogin              | No   |
| GET    | `/auth/me`                    | handleMe                 | Sí   |
| POST   | `/webhooks/email`             | handleWebhookEmail       | Secret |
| GET    | `/api/categorias`             | handleGetCategorias      | Sí   |
| POST   | `/api/categorias`             | handlePostCategoria      | Sí   |
| PUT    | `/api/categorias/:id`         | handlePutCategoria       | Sí   |
| DELETE | `/api/categorias/:id`         | handleDeleteCategoria    | Sí   |
| GET    | `/api/tareas`                 | handleGetTareas          | Sí   |
| GET    | `/api/tareas/:id`             | handleGetTarea           | Sí   |
| POST   | `/api/tareas`                 | handlePostTarea          | Sí   |
| PUT    | `/api/tareas/:id`             | handlePutTarea           | Sí   |
| PATCH  | `/api/tareas/:id/completar`   | handlePatchCompletar     | Sí   |
| DELETE | `/api/tareas/:id`             | handleDeleteTarea        | Sí   |

## CORS

Orígenes permitidos:
- `https://mytaskmyhabitpwa.pages.dev`
- `http://localhost:5173`
- `http://localhost:5174`

Todas las peticiones OPTIONS reciben 204 antes del dispatch.  
Los headers CORS se inyectan en la respuesta final (no en cada handler).

## Helpers compartidos (`src/index.js`)

### `requireAuth(request, env)` → `{ user } | { error: Response }`

Extrae el Bearer token del header `Authorization`, verifica la firma HS256 con
`crypto.subtle` y comprueba la expiración (`payload.exp`).  
Si todo es válido devuelve `{ user: payload }` (el payload JSON del JWT).  
En cualquier error devuelve `{ error: Response(401) }` listo para hacer `return result.error`.

### `resolveUser(username, env)` → `number | null`

Consulta `/rest/v1/usuarios?username=eq.<username>&select=id` en Supabase con
la service-role key. Devuelve el `id` numérico del usuario o `null` si no existe.

### `supabaseRequest(env, method, path, body?)` → `{ data, error }`

Wrapper genérico para la REST API de Supabase.  
Siempre incluye `Prefer: return=representation` para obtener el recurso creado/actualizado.  
`data` es el array/objeto parseado; `error` es `null` en caso de éxito o
`{ status, message }` en caso de fallo.

## Dependencias

| Paquete    | Uso                                              |
|------------|--------------------------------------------------|
| `bcryptjs` | Verificación de contraseñas en `handleLogin`     |
| `wrangler` | Dev/deploy (devDependency)                       |

## Historial de iteraciones

| Iteración | Cambios |
|-----------|---------|
| 1 — 2026-08-23 | Estructura base: wrangler.toml, package.json, src/index.js con router + CORS + stubs |
| 2 — 2026-08-23 | Helpers compartidos: requireAuth (HS256 Web Crypto), resolveUser, supabaseRequest |
| 3 — 2026-08-23 | handleLogin (bcrypt+signJWT), handleMe (Supabase), handleWebhookEmail (formData+SendGrid v3 API) |
| 4 — 2026-08-23 | handleGetCategorias, handlePostCategoria, handlePutCategoria, handleDeleteCategoria (port de categorias.js) |
| 5 — 2026-08-23 | handleGetTareas (sort JS), handleGetTarea, handlePostTarea, handlePutTarea, handlePatchCompletar, handleDeleteTarea (port de tareas.js) |
| 6 — 2026-08-23 | Deploy a producción: `wrangler deploy` → https://mytaskmyhabit-worker.fiztony.workers.dev |
