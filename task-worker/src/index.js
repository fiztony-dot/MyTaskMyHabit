import bcrypt from 'bcryptjs'

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

// ── Auth / JWT helpers ────────────────────────────────────────────────────────

// Parsea AUTH_USERS: "user:bcryptHash,user2:hash2" → [{ username, passwordHash }]
// Port exacto de server/src/config.js parseUsers()
function parseUsers(raw) {
  return (raw || '')
    .split(',')
    .map((pair) => pair.trim())
    .filter(Boolean)
    .map((pair) => {
      const sep = pair.indexOf(':')
      if (sep === -1) return null
      return { username: pair.slice(0, sep), passwordHash: pair.slice(sep + 1) }
    })
    .filter(Boolean)
}

// Codifica Uint8Array/ArrayBuffer a base64url (sin padding)
function b64urlEncode(bytes) {
  let binary = ''
  const view = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes)
  for (const b of view) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

// Firma un JWT HS256 compatible con jsonwebtoken
async function signJWT(payload, secret) {
  const headerB64  = b64urlEncode(new TextEncoder().encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })))
  const payloadB64 = b64urlEncode(new TextEncoder().encode(JSON.stringify(payload)))
  const signingInput = `${headerB64}.${payloadB64}`
  const key = await crypto.subtle.importKey(
    'raw', new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  )
  const sigBytes = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(signingInput))
  return `${signingInput}.${b64urlEncode(new Uint8Array(sigBytes))}`
}

// ── Webhook helpers (port literal de server/src/routes/webhooks.js) ────────────

function extraerEmail(fromStr) {
  if (!fromStr) return null
  const match = fromStr.match(/<([^>]+)>/)
  return match ? match[1].trim() : fromStr.trim()
}

function limpiarCuerpo(texto) {
  if (!texto) return null
  const lineas = texto.split('\n')
  const resultado = []
  for (const linea of lineas) {
    const trim = linea.trim()
    if (trim === '--' || trim === '—' || trim === '---') break
    if (trim.startsWith('>')) continue
    if (/^(On\s|El\s).+wrote\s*:/i.test(trim) || /^(On\s|El\s).+escribi[oó]\s*:/i.test(trim)) break
    resultado.push(linea)
  }
  const limpio = resultado.join('\n').trim()
  if (!limpio) return null
  return limpio.length > 1000 ? limpio.substring(0, 997) + '...' : limpio
}

// ── Handlers ──────────────────────────────────────────────────────────────────

async function healthCheck(_req, _env) {
  return json({ status: 'ok' })
}

async function handleLogin(request, env) {
  let body
  try { body = await request.json() } catch { body = {} }
  const { username, password } = body || {}

  if (!username || !password) {
    return json({ error: 'Se requieren username y password' }, 400)
  }

  const users = parseUsers(env.AUTH_USERS)
  const match = users.find((u) => u.username === username)
  if (!match) return json({ error: 'Usuario o contraseña incorrectos' }, 401)

  const valid = await bcrypt.compare(password, match.passwordHash)
  if (!valid) return json({ error: 'Usuario o contraseña incorrectos' }, 401)

  const now = Math.floor(Date.now() / 1000)
  const token = await signJWT(
    { sub: username, iat: now, exp: now + 30 * 24 * 3600 },
    env.JWT_SECRET
  )
  return json({ token, user: { username } })
}

async function handleMe(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error

  const path = `usuarios?username=eq.${encodeURIComponent(auth.user.sub)}&select=id,username,nombre_visible`
  const { data, error } = await supabaseRequest(env, 'GET', path)
  if (error || !data || data.length === 0) {
    return json({ error: 'Usuario no encontrado en la base de datos' }, 404)
  }
  const u = data[0]
  return json({ user: { id: Number(u.id), username: u.username, nombreVisible: u.nombre_visible } })
}

async function handleWebhookEmail(request, env) {
  const url = new URL(request.url)
  if (!env.WEBHOOK_SECRET || url.searchParams.get('secret') !== env.WEBHOOK_SECRET) {
    return json({ error: 'Forbidden' }, 403)
  }

  try {
    const formData = await request.formData()
    const subject = formData.get('subject') || ''
    const text    = formData.get('text')    || ''
    const from    = formData.get('from')    || ''

    const titulo          = (subject || 'Sin asunto').substring(0, 255)
    const descripcion     = limpiarCuerpo(text)
    const emailRemitente  = extraerEmail(from)

    const { data, error } = await supabaseRequest(env, 'POST', 'tareas_table', {
      titulo,
      descripcion,
      usuario_id:           1,
      categoria_id:         null,
      pendiente_clasificar: true,
      prioridad:            'MEDIA',
      esta_completada:      false,
    })

    if (error || !data || data.length === 0) {
      console.error('[webhook/email] Error insertando tarea:', error)
      return json({ ok: false, error: 'Internal error logged' })
    }

    const tareaId = data[0].id
    console.log(`[webhook/email] Tarea id=${tareaId} creada: "${titulo}"`)

    if (emailRemitente && env.SENDGRID_API_KEY && env.SENDGRID_FROM_EMAIL) {
      try {
        const emailText = [
          'Tu tarea ha sido creada en MyTaskMyHabit:',
          '',
          `📋 ${titulo}`,
          '',
          'Está pendiente de clasificar. Ábrela en la app para asignarle fecha, categoría y prioridad.',
          '',
          '— MyTaskMyHabit',
        ].join('\n')
        await fetch('https://api.sendgrid.com/v3/mail/send', {
          method: 'POST',
          headers: { Authorization: `Bearer ${env.SENDGRID_API_KEY}`, 'Content-Type': 'application/json' },
          body: JSON.stringify({
            personalizations: [{ to: [{ email: emailRemitente }] }],
            from:    { email: env.SENDGRID_FROM_EMAIL },
            subject: `✅ Tarea creada: ${titulo}`,
            content: [{ type: 'text/plain', value: emailText }],
          }),
        })
        console.log(`[webhook/email] Confirmación enviada a ${emailRemitente}`)
      } catch (emailErr) {
        console.error('[webhook/email] Error enviando confirmación:', emailErr.message)
      }
    } else {
      console.log('[webhook/email] Confirmación omitida (SENDGRID_API_KEY o FROM_EMAIL no configurados)')
    }

    return json({ ok: true, tareaId })
  } catch (err) {
    console.error('[webhook/email] Error procesando webhook:', err)
    return json({ ok: false, error: 'Internal error logged' })
  }
}

async function handleGetCategorias(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const path = `categorias_table?usuario_id=eq.${usuarioId}&select=id,titulo,icono,fecha_creacion,activa&order=fecha_creacion.asc`
  const { data, error } = await supabaseRequest(env, 'GET', path)
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data })
}

async function handlePostCategoria(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const { titulo, icono } = body || {}

  if (!titulo || titulo.trim() === '') {
    return json({ error: 'El campo titulo es obligatorio' }, 400)
  }

  const { data, error } = await supabaseRequest(env, 'POST', 'categorias_table', {
    usuario_id: usuarioId,
    titulo:     titulo.trim(),
    icono:      icono || 'list',
  })
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data: Array.isArray(data) ? data[0] : data }, 201)
}

async function handlePutCategoria(request, env, catId) {
  const id = parseInt(catId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const { data: existing } = await supabaseRequest(
    env, 'GET', `categorias_table?id=eq.${id}&usuario_id=eq.${usuarioId}&select=id`
  )
  if (!existing || existing.length === 0) return json({ error: 'Categoría no encontrada' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const { titulo, icono, activa } = body || {}

  const updates = {}
  if (titulo !== undefined) updates.titulo = titulo.trim()
  if (icono  !== undefined) updates.icono  = icono
  if (activa !== undefined) updates.activa = activa

  if (Object.keys(updates).length === 0) {
    return json({ error: 'No se proporcionaron campos para actualizar' }, 400)
  }

  const { data, error } = await supabaseRequest(
    env, 'PATCH', `categorias_table?id=eq.${id}&usuario_id=eq.${usuarioId}`, updates
  )
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data: Array.isArray(data) ? data[0] : data })
}

async function handleDeleteCategoria(request, env, catId) {
  const id = parseInt(catId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const { data, error } = await supabaseRequest(
    env, 'DELETE', `categorias_table?id=eq.${id}&usuario_id=eq.${usuarioId}`
  )
  if (error) return json({ error: error.message }, error.status || 500)
  if (!data || data.length === 0) return json({ error: 'Categoría no encontrada' }, 404)
  return json({ data: { deleted: true, id } })
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
