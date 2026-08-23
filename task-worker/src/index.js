// ── CORS ──────────────────────────────────────────────────────────────────────

const ALLOWED_ORIGINS = [
  'https://mytaskmyhabitpwa.pages.dev',
  'http://localhost:5173',
  'http://localhost:5174',
]

function corsHeaders(origin) {
  const allowed = ALLOWED_ORIGINS.includes(origin) ? origin : ALLOWED_ORIGINS[0]
  return {
    'Access-Control-Allow-Origin': allowed,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Credentials': 'true',
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function notFound() {
  return json({ error: 'Not Found' }, 404)
}

// Decodifica base64url → Uint8Array (sin padding ni chars URL-safe)
function b64url(str) {
  const padded = str + '==='.slice((str.length + 3) % 4)
  const base64 = padded.replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
}

// ── requireAuth ───────────────────────────────────────────────────────────────
// Extrae y verifica el JWT Bearer del header Authorization.
// Devuelve { user: payload } si es válido o { error: Response } si no lo es.

async function requireAuth(request, env) {
  const authHeader = request.headers.get('Authorization') ?? ''
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : ''

  if (!token) {
    return { error: json({ error: 'Falta el token de autenticación' }, 401) }
  }

  try {
    const parts = token.split('.')
    if (parts.length !== 3) throw new Error('malformed')

    const [headerB64, payloadB64, sigB64] = parts
    const payload = JSON.parse(new TextDecoder().decode(b64url(payloadB64)))

    if (payload.exp && payload.exp * 1000 < Date.now()) {
      return { error: json({ error: 'Token inválido o expirado' }, 401) }
    }

    const keyBytes = new TextEncoder().encode(env.JWT_SECRET)
    const key = await crypto.subtle.importKey(
      'raw', keyBytes, { name: 'HMAC', hash: 'SHA-256' }, false, ['verify']
    )
    const signedData = new TextEncoder().encode(headerB64 + '.' + payloadB64)
    const valid = await crypto.subtle.verify('HMAC', key, b64url(sigB64), signedData)

    if (!valid) return { error: json({ error: 'Token inválido o expirado' }, 401) }

    return { user: payload }
  } catch {
    return { error: json({ error: 'Token inválido o expirado' }, 401) }
  }
}

// ── resolveUser ───────────────────────────────────────────────────────────────
// Busca el usuario por username en Supabase y devuelve su id numérico o null.

async function resolveUser(username, env) {
  const url = `${env.SUPABASE_URL}/rest/v1/usuarios?username=eq.${encodeURIComponent(username)}&select=id`
  const res = await fetch(url, {
    headers: {
      apikey:        env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
    },
  })
  const rows = await res.json()
  if (!Array.isArray(rows) || rows.length === 0) return null
  return Number(rows[0].id)
}

// ── supabaseRequest ───────────────────────────────────────────────────────────
// Realiza una petición autenticada a la REST API de Supabase.
// Devuelve { data, error }.

async function supabaseRequest(env, method, path, body) {
  const url = `${env.SUPABASE_URL}/rest/v1/${path}`
  const headers = {
    apikey:           env.SUPABASE_SERVICE_ROLE_KEY,
    Authorization:    `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
    'Content-Type':   'application/json',
    Prefer:           'return=representation',
  }
  const init = { method, headers }
  if (body !== undefined) init.body = JSON.stringify(body)

  try {
    const res = await fetch(url, init)
    if (!res.ok) {
      const text = await res.text()
      return { data: null, error: { status: res.status, message: text } }
    }
    const data = await res.json()
    return { data, error: null }
  } catch (err) {
    return { data: null, error: { status: 503, message: err.message } }
  }
}

// ── Stub handlers ─────────────────────────────────────────────────────────────

async function healthCheck(_req, _env) {
  return json({ status: 'ok' })
}

async function handleLogin(_req, _env) {
  return json({ ok: true })
}

async function handleMe(_req, _env) {
  return json({ ok: true })
}

async function handleWebhookEmail(_req, _env) {
  return json({ ok: true })
}

async function handleGetCategorias(_req, _env) {
  return json([])
}

async function handlePostCategoria(_req, _env) {
  return json({ ok: true }, 201)
}

async function handlePutCategoria(_req, _env, _id) {
  return json({ ok: true })
}

async function handleDeleteCategoria(_req, _env, _id) {
  return json({ ok: true })
}

async function handleGetTareas(_req, _env) {
  return json([])
}

async function handleGetTarea(_req, _env, _id) {
  return json({ ok: true })
}

async function handlePostTarea(_req, _env) {
  return json({ ok: true }, 201)
}

async function handlePutTarea(_req, _env, _id) {
  return json({ ok: true })
}

async function handlePatchCompletar(_req, _env, _id) {
  return json({ ok: true })
}

async function handleDeleteTarea(_req, _env, _id) {
  return json({ ok: true })
}

// ── Router ────────────────────────────────────────────────────────────────────

async function dispatch(request, env) {
  const { pathname } = new URL(request.url)
  const method = request.method
  // Split path and drop empty segments: '/api/tareas/42' → ['api','tareas','42']
  const p = pathname.split('/').filter(Boolean)

  // Health
  if (pathname === '/health' && method === 'GET') return healthCheck(request, env)

  // Auth
  if (pathname === '/auth/login' && method === 'POST') return handleLogin(request, env)
  if (pathname === '/auth/me'    && method === 'GET')  return handleMe(request, env)

  // Webhooks
  if (pathname === '/webhooks/email' && method === 'POST') return handleWebhookEmail(request, env)

  // Categorías
  if (pathname === '/api/categorias' && method === 'GET')  return handleGetCategorias(request, env)
  if (pathname === '/api/categorias' && method === 'POST') return handlePostCategoria(request, env)
  if (p[0]==='api' && p[1]==='categorias' && p[2] && p.length===3) {
    if (method === 'PUT')    return handlePutCategoria(request, env, p[2])
    if (method === 'DELETE') return handleDeleteCategoria(request, env, p[2])
  }

  // Tareas
  if (pathname === '/api/tareas' && method === 'GET')  return handleGetTareas(request, env)
  if (pathname === '/api/tareas' && method === 'POST') return handlePostTarea(request, env)
  if (p[0]==='api' && p[1]==='tareas' && p[2]) {
    if (p[3]==='completar' && p.length===4 && method==='PATCH')
      return handlePatchCompletar(request, env, p[2])
    if (p.length===3) {
      if (method === 'GET')    return handleGetTarea(request, env, p[2])
      if (method === 'PUT')    return handlePutTarea(request, env, p[2])
      if (method === 'DELETE') return handleDeleteTarea(request, env, p[2])
    }
  }

  return notFound()
}

// ── Entry point ───────────────────────────────────────────────────────────────

export default {
  async fetch(request, env) {
    const origin = request.headers.get('Origin') ?? ''
    const cors = corsHeaders(origin)

    // CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: cors })
    }

    // Dispatch and inject CORS headers into every response
    const response = await dispatch(request, env)
    const headers = new Headers(response.headers)
    for (const [k, v] of Object.entries(cors)) headers.set(k, v)
    return new Response(response.body, { status: response.status, headers })
  },
}
