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

// ── Tareas helpers ───────────────────────────────────────────────────────────

const TAREAS_SELECT = [
  'id', 'titulo', 'descripcion', 'esta_completada', 'prioridad', 'fecha_creacion',
  'fecha_limite', 'hora_limite', 'categoria_id', 'repeticion', 'pendiente_clasificar',
  'repeticion_fin', 'repeticion_veces', 'repeticion_contador',
].join(',')
const PRIORIDADES = ['ALTA', 'MEDIA', 'BAJA']
const ORDEN_PRIORIDAD = { ALTA: 1, MEDIA: 2, BAJA: 3 }

// ── Web Push / VAPID (RFC 8291 + RFC 8292 + RFC 8188) ────────────────────────

// Cifra un mensaje con la clave de suscripción push del cliente (RFC 8291 + aes128gcm).
// Devuelve un Uint8Array con el body listo para enviar al push service.
async function encryptWebPushMessage(subscription, payload) {
  const payloadStr = typeof payload === 'string' ? payload : JSON.stringify(payload)
  const authSecret      = b64url(subscription.auth)
  const receiverPubBytes = b64url(subscription.p256dh)

  // 1. Par de claves ECDH efímero del servidor de aplicaciones
  const senderPair = await crypto.subtle.generateKey(
    { name: 'ECDH', namedCurve: 'P-256' }, true, ['deriveBits']
  )
  const senderPubRaw = new Uint8Array(await crypto.subtle.exportKey('raw', senderPair.publicKey))

  // 2. Clave pública del receptor importada para ECDH
  const receiverPubKey = await crypto.subtle.importKey(
    'raw', receiverPubBytes, { name: 'ECDH', namedCurve: 'P-256' }, false, []
  )

  // 3. Secreto compartido ECDH
  const ecdhSecret = await crypto.subtle.deriveBits(
    { name: 'ECDH', public: receiverPubKey }, senderPair.privateKey, 256
  )

  // 4. Salt aleatorio (16 bytes)
  const salt = crypto.getRandomValues(new Uint8Array(16))

  // 5. IKM según RFC 8291 §3.3:
  //    PRK = HKDF-Extract(salt=auth_secret, IKM=ecdh_secret)
  //    IKM = HKDF-Expand(PRK, info="WebPush: info\0" + ua_pub + as_pub, 32)
  //    → con WebCrypto, HKDF(salt=auth_secret, ikm=ecdh_secret, info=keyInfo, 32)
  const keyInfoPrefix = new TextEncoder().encode('WebPush: info\x00')
  const keyInfo = new Uint8Array(keyInfoPrefix.length + receiverPubBytes.length + senderPubRaw.length)
  keyInfo.set(keyInfoPrefix)
  keyInfo.set(receiverPubBytes, keyInfoPrefix.length)
  keyInfo.set(senderPubRaw, keyInfoPrefix.length + receiverPubBytes.length)

  const ecdhKey = await crypto.subtle.importKey('raw', ecdhSecret, 'HKDF', false, ['deriveBits'])
  const ikm = await crypto.subtle.deriveBits(
    { name: 'HKDF', salt: authSecret, info: keyInfo, hash: 'SHA-256' }, ecdhKey, 256
  )

  // 6. CEK (16 bytes) y nonce (12 bytes) según RFC 8188:
  //    PRK2 = HKDF-Extract(salt=random_salt, IKM=ikm)
  //    CEK  = HKDF-Expand(PRK2, "Content-Encoding: aes128gcm\0", 16)
  //    NONCE= HKDF-Expand(PRK2, "Content-Encoding: nonce\0", 12)
  const ikmKey    = await crypto.subtle.importKey('raw', ikm, 'HKDF', false, ['deriveBits'])
  const cekInfo   = new TextEncoder().encode('Content-Encoding: aes128gcm\x00')
  const nonceInfo = new TextEncoder().encode('Content-Encoding: nonce\x00')
  const cekBits   = await crypto.subtle.deriveBits(
    { name: 'HKDF', salt, info: cekInfo,   hash: 'SHA-256' }, ikmKey, 128
  )
  const nonceBits = await crypto.subtle.deriveBits(
    { name: 'HKDF', salt, info: nonceInfo, hash: 'SHA-256' }, ikmKey, 96
  )

  // 7. AES-128-GCM: cifrar payload + byte delimitador 0x02 (último registro RFC 8188)
  const aesKey = await crypto.subtle.importKey('raw', cekBits, 'AES-GCM', false, ['encrypt'])
  const ptBytes = new TextEncoder().encode(payloadStr)
  const padded  = new Uint8Array(ptBytes.length + 1)
  padded.set(ptBytes)
  padded[ptBytes.length] = 0x02  // delimiter: last record
  const ciphertext = new Uint8Array(
    await crypto.subtle.encrypt({ name: 'AES-GCM', iv: nonceBits }, aesKey, padded)
  )

  // 8. Header aes128gcm (RFC 8188): salt(16) + rs(4 BE) + idlen(1) + keyid(senderPubRaw)
  const rs = 4096
  const header = new Uint8Array(16 + 4 + 1 + senderPubRaw.length)
  header.set(salt, 0)
  new DataView(header.buffer).setUint32(16, rs, false)
  header[20] = senderPubRaw.length  // idlen = 65 (uncompressed P-256)
  header.set(senderPubRaw, 21)

  const body = new Uint8Array(header.length + ciphertext.length)
  body.set(header, 0)
  body.set(ciphertext, header.length)
  return body
}

// Firma un JWT ES256 (ECDSA P-256 SHA-256) para VAPID (RFC 8292).
// privateKeyJwk: objeto JWK con crv:P-256, kty:EC, d (y x, y).
async function signVapidJwt(claims, privateKeyJwk) {
  const privateKey = await crypto.subtle.importKey(
    'jwk', privateKeyJwk,
    { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign']
  )
  const header  = b64urlEncode(new TextEncoder().encode(JSON.stringify({ typ: 'JWT', alg: 'ES256' })))
  const payload = b64urlEncode(new TextEncoder().encode(JSON.stringify(claims)))
  const sigInput = `${header}.${payload}`
  // WebCrypto devuelve la firma en formato IEEE P1363 (R||S, 64 bytes), que es el formato JWT ES256.
  const sigBytes = new Uint8Array(
    await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, privateKey,
      new TextEncoder().encode(sigInput))
  )
  return `${sigInput}.${b64urlEncode(sigBytes)}`
}

// Envía una notificación push a una suscripción concreta.
// Devuelve { ok: true } | { expired: true } | { error: string }.
async function sendWebPush(env, subscription, payload) {
  let vapidPrivateJwk
  try { vapidPrivateJwk = JSON.parse(env.VAPID_PRIVATE_KEY_JWK) } catch {
    return { error: 'VAPID_PRIVATE_KEY_JWK no configurado o inválido' }
  }
  if (!env.VAPID_PUBLIC_KEY || !env.VAPID_SUBJECT) {
    return { error: 'VAPID_PUBLIC_KEY o VAPID_SUBJECT no configurados' }
  }

  const endpointUrl = new URL(subscription.endpoint)
  const audience = `${endpointUrl.protocol}//${endpointUrl.host}`
  const now = Math.floor(Date.now() / 1000)
  const jwt = await signVapidJwt(
    { aud: audience, exp: now + 43200, sub: env.VAPID_SUBJECT },
    vapidPrivateJwk
  )

  const body = await encryptWebPushMessage(subscription, payload)

  const res = await fetch(subscription.endpoint, {
    method: 'POST',
    headers: {
      Authorization:      `vapid t=${jwt},k=${env.VAPID_PUBLIC_KEY}`,
      'Content-Type':     'application/octet-stream',
      'Content-Encoding': 'aes128gcm',
      TTL:                '86400',
    },
    body,
  })

  if (res.status === 404 || res.status === 410) return { expired: true, status: res.status }
  if (!res.ok) {
    const text = await res.text()
    console.error('[push] Error HTTP del push service:', res.status, text.slice(0, 200))
    return { error: `HTTP ${res.status}` }
  }
  return { ok: true }
}

// Envía una notificación push a todas las suscripciones de un usuario.
// Elimina automáticamente las suscripciones expiradas (410 Gone).
// Devuelve { enviadas, intentadas } para que el caller pueda actuar según el resultado.
async function sendPushToUser(env, usuarioId, payload) {
  const { data: subs, error } = await supabaseRequest(
    env, 'GET', `push_subscriptions?usuario_id=eq.${usuarioId}&select=id,endpoint,p256dh,auth`
  )
  if (error || !subs || subs.length === 0) return { enviadas: 0, intentadas: 0 }

  let enviadas = 0
  for (const sub of subs) {
    const result = await sendWebPush(env, sub, payload)
    if (result.ok) {
      enviadas++
      await supabaseRequest(env, 'PATCH',
        `push_subscriptions?id=eq.${sub.id}`,
        { ultimo_envio_ok: new Date().toISOString() }
      )
    } else if (result.expired) {
      await supabaseRequest(env, 'DELETE', `push_subscriptions?id=eq.${sub.id}`)
      console.log(`[push] Suscripción expirada eliminada id=${sub.id}`)
    }
  }
  return { enviadas, intentadas: subs.length }
}

// Hora actual en Europe/Madrid → { date: "YYYY-MM-DD", hTotal: minutos desde medianoche }
function madridNow() {
  const now   = new Date()
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Madrid',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).formatToParts(now)
  const get  = (type) => parts.find(p => p.type === type)?.value ?? '0'
  const hRaw = parseInt(get('hour'), 10)
  const h    = hRaw === 24 ? 0 : hRaw   // 'en-CA' nunca devuelve 24, pero por si acaso
  const m    = parseInt(get('minute'), 10)
  return {
    date:   `${get('year')}-${get('month')}-${get('day')}`,
    hTotal: h * 60 + m,
  }
}

// ── Adjuntos (attachments) ─────────────────────────────────────────────────────

const ADJUNTOS_BUCKET = 'tarea-adjuntos'
const ADJUNTO_MAX_BYTES = 10 * 1024 * 1024 // 10 MB
const ADJUNTO_SIGNED_URL_TTL = 3600 // 1 hora (segundos)

// Tipos MIME permitidos. Imágenes: jpg/png/gif/webp. Documentos: pdf/doc/docx/xls/xlsx/txt.
const ADJUNTO_MIME_PERMITIDOS = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'application/pdf',
  'application/msword',                                                       // .doc
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',  // .docx
  'application/vnd.ms-excel',                                                 // .xls
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',        // .xlsx
  'text/plain',                                                               // .txt
])

// Extensión → MIME de respaldo, para clientes que no envían Content-Type fiable.
const ADJUNTO_EXT_MIME = {
  jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', gif: 'image/gif', webp: 'image/webp',
  pdf: 'application/pdf', txt: 'text/plain',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
}
const ADJUNTOS_SELECT =
  'id,tarea_id,usuario_id,nombre_fichero,tipo_mime,tamaño_bytes,storage_path,url_publica,creado_en'

// Resuelve el MIME real combinando el declarado por el cliente y la extensión del nombre.
function resolverMime(declarado, nombre) {
  if (declarado && ADJUNTO_MIME_PERMITIDOS.has(declarado)) return declarado
  const ext = (nombre.split('.').pop() || '').toLowerCase()
  return ADJUNTO_EXT_MIME[ext] || declarado || 'application/octet-stream'
}

// Limpia el nombre de fichero para usarlo en un path de Storage (sin separadores ni chars raros).
function sanitizarNombre(nombre) {
  const base = (nombre || 'fichero').split(/[\\/]/).pop()
  const limpio = base.replace(/[^\w.\-() ]/g, '_').trim()
  return limpio || 'fichero'
}

// Crea el bucket privado si no existe (idempotente). No lanza si ya existe.
async function ensureAdjuntosBucket(env) {
  const res = await fetch(`${env.SUPABASE_URL}/storage/v1/bucket`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      id: ADJUNTOS_BUCKET,
      name: ADJUNTOS_BUCKET,
      public: false,
      file_size_limit: ADJUNTO_MAX_BYTES,
    }),
  })
  // 200/201 = creado; 409 = ya existía. Ambos son OK.
  if (!res.ok && res.status !== 409) {
    const text = await res.text()
    // 400 con "already exists" en algunas versiones — tratar como OK
    if (!/exist/i.test(text)) {
      console.error('[adjuntos] No se pudo crear el bucket:', res.status, text)
    }
  }
}

// Sube bytes a Storage. Devuelve { error } (null si OK).
async function storageUpload(env, path, bytes, contentType) {
  const url = `${env.SUPABASE_URL}/storage/v1/object/${ADJUNTOS_BUCKET}/${path}`
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
      'Content-Type': contentType,
      'x-upsert': 'true',
    },
    body: bytes,
  })
  if (!res.ok) {
    const text = await res.text()
    return { error: { status: res.status, message: text } }
  }
  return { error: null }
}

// Borra un objeto de Storage. Devuelve { error } (null si OK, ignora 404).
async function storageDelete(env, path) {
  const url = `${env.SUPABASE_URL}/storage/v1/object/${ADJUNTOS_BUCKET}/${path}`
  const res = await fetch(url, {
    method: 'DELETE',
    headers: {
      apikey: env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
    },
  })
  if (!res.ok && res.status !== 404) {
    const text = await res.text()
    return { error: { status: res.status, message: text } }
  }
  return { error: null }
}

// Genera una URL firmada (válida ADJUNTO_SIGNED_URL_TTL segundos) para un objeto.
// Devuelve la URL absoluta o null si falla.
async function storageSignedUrl(env, path) {
  const url = `${env.SUPABASE_URL}/storage/v1/object/sign/${ADJUNTOS_BUCKET}/${path}`
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ expiresIn: ADJUNTO_SIGNED_URL_TTL }),
  })
  if (!res.ok) {
    console.error('[adjuntos] Error firmando URL:', res.status, await res.text())
    return null
  }
  const body = await res.json()
  // signedURL es relativa: "/object/sign/<bucket>/<path>?token=..."
  const signed = body.signedURL || body.signedUrl
  if (!signed) return null
  return `${env.SUPABASE_URL}/storage/v1${signed.startsWith('/') ? '' : '/'}${signed}`
}

// Verifica que la tarea pertenece al usuario. Devuelve true/false.
async function tareaPerteneceAUsuario(env, tareaId, usuarioId) {
  const { data } = await supabaseRequest(
    env, 'GET', `tareas_table?id=eq.${tareaId}&usuario_id=eq.${usuarioId}&select=id`
  )
  return Array.isArray(data) && data.length > 0
}

// Serializa una fila de adjunto añadiendo la URL firmada fresca.
async function adjuntoToDto(env, row) {
  const signed = await storageSignedUrl(env, row.storage_path)
  return {
    id: Number(row.id),
    tarea_id: Number(row.tarea_id),
    nombre_fichero: row.nombre_fichero,
    tipo_mime: row.tipo_mime,
    tamano_bytes: Number(row.tamaño_bytes),
    creado_en: row.creado_en,
    url_firmada: signed || row.url_publica,
  }
}

// ── Handlers ──────────────────────────────────────────────────────────────────

async function healthCheck(req, _env) {
  const body = req.method === 'HEAD' ? null : JSON.stringify({ status: 'ok' })
  return new Response(body, {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
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
    { sub: username, iat: now, exp: now + 365 * 24 * 3600 },
    env.JWT_SECRET
  )
  return json({ token, user: { username } })
}

async function handleRealtimeToken(request, env) {
  // requireAuth devuelve { user } si el JWT es válido o { error: Response(401) } si no.
  // Cualquier fallo de auth retorna 401, nunca 500.
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error

  try {
    const usuarioId = await resolveUser(auth.user.sub, env)
    if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

    if (!env.SUPABASE_JWT_SECRET) {
      return json({ error: 'SUPABASE_JWT_SECRET no configurado en el Worker' }, 500)
    }

    const now = Math.floor(Date.now() / 1000)
    const exp = now + 3600 // 1 hora
    const token = await signJWT(
      {
        role: 'authenticated',
        sub: String(usuarioId),
        aud: 'authenticated',
        iat: now,
        exp,
      },
      env.SUPABASE_JWT_SECRET
    )

    return json({ token, expiresAt: exp })
  } catch (err) {
    console.error('[realtime-token] Error generando token:', err)
    return json({ error: 'Error interno generando el token de Realtime' }, 500)
  }
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

    // Notificación push (no bloquea si falla)
    sendPushToUser(env, 1, {
      title: '📧 Nueva tarea por email',
      body:  titulo.length > 80 ? titulo.slice(0, 77) + '…' : titulo,
      icon:  '/icons/icon-192.png',
      url:   '/',
      tag:   `email-task-${tareaId}`,
    }).catch((err) => console.error('[webhook/email] Error enviando push:', err.message))

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

async function handleGetTareas(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const url = new URL(request.url)
  const pendientes = url.searchParams.get('pendientes') === 'true'

  let path = `tareas_table?usuario_id=eq.${usuarioId}&select=${TAREAS_SELECT}`
  if (pendientes) path += '&esta_completada=eq.false'

  const { data, error } = await supabaseRequest(env, 'GET', path)
  if (error) return json({ error: error.message }, error.status || 500)

  const rows = (data || []).sort(
    (a, b) =>
      (ORDEN_PRIORIDAD[a.prioridad] ?? 9) - (ORDEN_PRIORIDAD[b.prioridad] ?? 9) ||
      new Date(a.fecha_creacion) - new Date(b.fecha_creacion)
  )

  // Anotar cada tarea con su nº de adjuntos (una sola consulta agregada).
  // Si la tabla aún no existe (migración no aplicada), se ignora silenciosamente.
  try {
    const { data: adj } = await supabaseRequest(
      env, 'GET', `tarea_adjuntos?usuario_id=eq.${usuarioId}&select=tarea_id`
    )
    if (Array.isArray(adj)) {
      const counts = {}
      for (const a of adj) counts[a.tarea_id] = (counts[a.tarea_id] || 0) + 1
      for (const r of rows) r.adjuntos_count = counts[r.id] || 0
    }
  } catch {
    /* tabla inexistente o error transitorio — no bloquea la lista */
  }

  return json({ data: rows })
}

async function handleGetTarea(request, env, tareaId) {
  const id = parseInt(tareaId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const { data, error } = await supabaseRequest(
    env, 'GET', `tareas_table?id=eq.${id}&usuario_id=eq.${usuarioId}&select=${TAREAS_SELECT}`
  )
  if (error) return json({ error: error.message }, error.status || 500)
  if (!data || data.length === 0) return json({ error: 'Tarea no encontrada' }, 404)
  return json({ data: data[0] })
}

async function handlePostTarea(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const {
    titulo, descripcion, prioridad, fecha_limite, hora_limite,
    categoria_id, repeticion, pendiente_clasificar,
    repeticion_fin, repeticion_veces,
  } = body || {}

  if (!titulo || titulo.trim() === '') {
    return json({ error: 'El campo titulo es obligatorio' }, 400)
  }

  const priori = prioridad ? String(prioridad).toUpperCase() : 'MEDIA'
  if (!PRIORIDADES.includes(priori)) {
    return json({ error: 'Prioridad inválida. Valores: ALTA, MEDIA, BAJA' }, 400)
  }

  if (categoria_id != null) {
    const { data: catCheck } = await supabaseRequest(
      env, 'GET', `categorias_table?id=eq.${categoria_id}&usuario_id=eq.${usuarioId}&select=id`
    )
    if (!catCheck || catCheck.length === 0) {
      return json({ error: 'La categoría especificada no existe o no pertenece al usuario' }, 400)
    }
  }

  const { data, error } = await supabaseRequest(env, 'POST', 'tareas_table', {
    usuario_id:           usuarioId,
    titulo:               titulo.trim(),
    descripcion:          descripcion || null,
    prioridad:            priori,
    fecha_limite:         fecha_limite || null,
    hora_limite:          hora_limite || null,
    categoria_id:         categoria_id || null,
    repeticion:           repeticion || 'Sin repetición',
    pendiente_clasificar: pendiente_clasificar || false,
    repeticion_fin:       repeticion_fin || null,
    repeticion_veces:     repeticion_veces || null,
  })
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data: Array.isArray(data) ? data[0] : data }, 201)
}

async function handlePutTarea(request, env, tareaId) {
  const id = parseInt(tareaId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const { data: existing } = await supabaseRequest(
    env, 'GET', `tareas_table?id=eq.${id}&usuario_id=eq.${usuarioId}&select=id`
  )
  if (!existing || existing.length === 0) return json({ error: 'Tarea no encontrada' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }

  const allowedFields = [
    'titulo', 'descripcion', 'prioridad', 'fecha_limite', 'hora_limite',
    'categoria_id', 'repeticion', 'pendiente_clasificar', 'esta_completada',
    'repeticion_fin', 'repeticion_veces', 'repeticion_contador',
  ]
  const updates = {}
  for (const field of allowedFields) {
    if ((body || {})[field] === undefined) continue
    let val = body[field]
    if (field === 'prioridad') {
      val = String(val).toUpperCase()
      if (!PRIORIDADES.includes(val)) return json({ error: 'Prioridad inválida' }, 400)
    }
    if (field === 'categoria_id' && val != null) {
      const { data: catCheck } = await supabaseRequest(
        env, 'GET', `categorias_table?id=eq.${val}&usuario_id=eq.${usuarioId}&select=id`
      )
      if (!catCheck || catCheck.length === 0) return json({ error: 'Categoría no encontrada' }, 400)
    }
    updates[field] = val
  }

  if (Object.keys(updates).length === 0) {
    return json({ error: 'No se proporcionaron campos para actualizar' }, 400)
  }

  const { data, error } = await supabaseRequest(
    env, 'PATCH', `tareas_table?id=eq.${id}&usuario_id=eq.${usuarioId}`, updates
  )
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data: Array.isArray(data) ? data[0] : data })
}

async function handlePatchCompletar(request, env, tareaId) {
  const id = parseInt(tareaId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const { esta_completada } = body || {}

  if (typeof esta_completada !== 'boolean') {
    return json({ error: 'El campo esta_completada debe ser boolean' }, 400)
  }

  const { data, error } = await supabaseRequest(
    env, 'PATCH', `tareas_table?id=eq.${id}&usuario_id=eq.${usuarioId}`, { esta_completada }
  )
  if (error) return json({ error: error.message }, error.status || 500)
  if (!data || data.length === 0) return json({ error: 'Tarea no encontrada' }, 404)
  return json({ data: Array.isArray(data) ? data[0] : data })
}

async function handleDeleteTarea(request, env, tareaId) {
  const id = parseInt(tareaId)
  if (isNaN(id)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  const { data, error } = await supabaseRequest(
    env, 'DELETE', `tareas_table?id=eq.${id}&usuario_id=eq.${usuarioId}`
  )
  if (error) return json({ error: error.message }, error.status || 500)
  if (!data || data.length === 0) return json({ error: 'Tarea no encontrada' }, 404)
  return json({ data: { deleted: true, id } })
}

// ── Handlers de adjuntos ────────────────────────────────────────────────────────

async function handleGetAdjuntos(request, env, tareaIdRaw) {
  const tareaId = parseInt(tareaIdRaw)
  if (isNaN(tareaId)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  if (!(await tareaPerteneceAUsuario(env, tareaId, usuarioId))) {
    return json({ error: 'Tarea no encontrada' }, 404)
  }

  const path = `tarea_adjuntos?tarea_id=eq.${tareaId}&usuario_id=eq.${usuarioId}&select=${ADJUNTOS_SELECT}&order=creado_en.asc`
  const { data, error } = await supabaseRequest(env, 'GET', path)
  if (error) return json({ error: error.message }, error.status || 500)

  const dtos = await Promise.all((data || []).map((row) => adjuntoToDto(env, row)))
  return json({ data: dtos })
}

async function handlePostAdjunto(request, env, tareaIdRaw) {
  const tareaId = parseInt(tareaIdRaw)
  if (isNaN(tareaId)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  if (!(await tareaPerteneceAUsuario(env, tareaId, usuarioId))) {
    return json({ error: 'Tarea no encontrada' }, 404)
  }

  let formData
  try {
    formData = await request.formData()
  } catch {
    return json({ error: 'Se esperaba multipart/form-data' }, 400)
  }

  const file = formData.get('file') || formData.get('adjunto') || formData.get('archivo')
  if (!file || typeof file === 'string') {
    return json({ error: 'Falta el fichero (campo "file")' }, 400)
  }

  const nombreOriginal = file.name || 'fichero'
  const mime = resolverMime(file.type, nombreOriginal)

  if (!ADJUNTO_MIME_PERMITIDOS.has(mime)) {
    return json(
      { error: 'Tipo de fichero no permitido. Se aceptan imágenes (jpg, png, gif, webp) y documentos (pdf, doc, docx, xls, xlsx, txt).' },
      400
    )
  }

  const bytes = new Uint8Array(await file.arrayBuffer())
  if (bytes.byteLength === 0) {
    return json({ error: 'El fichero está vacío' }, 400)
  }
  if (bytes.byteLength > ADJUNTO_MAX_BYTES) {
    return json({ error: 'El fichero supera el límite de 10MB' }, 413)
  }

  // Asegurar bucket (idempotente) y subir. Path: {usuario_id}/{tarea_id}/{timestamp}_{nombre}
  await ensureAdjuntosBucket(env)
  const nombreLimpio = sanitizarNombre(nombreOriginal)
  const storagePath = `${usuarioId}/${tareaId}/${Date.now()}_${nombreLimpio}`

  const { error: upErr } = await storageUpload(env, storagePath, bytes, mime)
  if (upErr) {
    console.error('[adjuntos] Error subiendo a Storage:', upErr)
    return json({ error: 'No se pudo subir el fichero al almacenamiento' }, upErr.status || 500)
  }

  // URL pública base (aunque el bucket sea privado; se sirve vía URL firmada).
  const urlPublica = `${env.SUPABASE_URL}/storage/v1/object/${ADJUNTOS_BUCKET}/${storagePath}`

  const { data, error } = await supabaseRequest(env, 'POST', 'tarea_adjuntos', {
    tarea_id:       tareaId,
    usuario_id:     usuarioId,
    nombre_fichero: nombreOriginal.substring(0, 255),
    tipo_mime:      mime,
    tamaño_bytes:   bytes.byteLength,
    storage_path:   storagePath,
    url_publica:    urlPublica,
  })

  if (error || !data || data.length === 0) {
    // Rollback del fichero subido si la fila no se pudo insertar
    await storageDelete(env, storagePath)
    console.error('[adjuntos] Error insertando fila:', error)
    return json({ error: 'No se pudo registrar el adjunto' }, error?.status || 500)
  }

  const dto = await adjuntoToDto(env, data[0])
  return json({ data: dto }, 201)
}

async function handleDeleteAdjunto(request, env, tareaIdRaw, adjuntoIdRaw) {
  const tareaId = parseInt(tareaIdRaw)
  const adjuntoId = parseInt(adjuntoIdRaw)
  if (isNaN(tareaId) || isNaN(adjuntoId)) return json({ error: 'ID inválido' }, 400)

  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado en la base de datos' }, 404)

  // Buscar el adjunto validando pertenencia (usuario + tarea)
  const { data: filas, error: getErr } = await supabaseRequest(
    env, 'GET',
    `tarea_adjuntos?id=eq.${adjuntoId}&tarea_id=eq.${tareaId}&usuario_id=eq.${usuarioId}&select=id,storage_path`
  )
  if (getErr) return json({ error: getErr.message }, getErr.status || 500)
  if (!filas || filas.length === 0) return json({ error: 'Adjunto no encontrado' }, 404)

  const storagePath = filas[0].storage_path

  // Borrar primero de Storage (si falla, no borramos la fila para no dejar huérfanos invisibles)
  const { error: delStorageErr } = await storageDelete(env, storagePath)
  if (delStorageErr) {
    console.error('[adjuntos] Error borrando de Storage:', delStorageErr)
    return json({ error: 'No se pudo eliminar el fichero del almacenamiento' }, delStorageErr.status || 500)
  }

  const { data, error } = await supabaseRequest(
    env, 'DELETE',
    `tarea_adjuntos?id=eq.${adjuntoId}&tarea_id=eq.${tareaId}&usuario_id=eq.${usuarioId}`
  )
  if (error) return json({ error: error.message }, error.status || 500)
  if (!data || data.length === 0) return json({ error: 'Adjunto no encontrado' }, 404)
  return json({ data: { deleted: true, id: adjuntoId } })
}

// ── Push subscription handlers ────────────────────────────────────────────────

async function handlePushSubscribe(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const { endpoint, p256dh, auth: authKey, userAgent } = body || {}
  if (!endpoint || !p256dh || !authKey) {
    return json({ error: 'Faltan campos: endpoint, p256dh, auth' }, 400)
  }

  // Upsert: si ya existe el endpoint, actualiza las claves
  const { data, error } = await supabaseRequest(env, 'POST', 'push_subscriptions', {
    usuario_id: usuarioId,
    endpoint,
    p256dh,
    auth: authKey,
    user_agent: userAgent || null,
  })
  if (error) {
    // 409 Conflict = endpoint ya existe: actualizar claves
    if (error.status === 409 || (error.message && error.message.includes('unique'))) {
      const { data: upd, error: updErr } = await supabaseRequest(
        env, 'PATCH',
        `push_subscriptions?endpoint=eq.${encodeURIComponent(endpoint)}&usuario_id=eq.${usuarioId}`,
        { p256dh, auth: authKey, user_agent: userAgent || null }
      )
      if (updErr) return json({ error: updErr.message }, updErr.status || 500)
      return json({ data: Array.isArray(upd) ? upd[0] : upd })
    }
    return json({ error: error.message }, error.status || 500)
  }
  return json({ data: Array.isArray(data) ? data[0] : data }, 201)
}

async function handlePushUnsubscribe(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado' }, 404)

  let body
  try { body = await request.json() } catch { body = {} }
  const { endpoint } = body || {}
  if (!endpoint) return json({ error: 'Falta endpoint' }, 400)

  const { error } = await supabaseRequest(
    env, 'DELETE',
    `push_subscriptions?endpoint=eq.${encodeURIComponent(endpoint)}&usuario_id=eq.${usuarioId}`
  )
  if (error) return json({ error: error.message }, error.status || 500)
  return json({ data: { deleted: true } })
}

// ── Scheduled: resumen diario de tareas ──────────────────────────────────────

async function handleScheduledDailySummary(env) {
  // ── 1. Comprobar ventana horaria en Europe/Madrid ──────────────────────────
  const madrid      = madridNow()
  const digestHora  = (env.DIGEST_HORA || '08:30').trim()
  const [dh, dm]    = digestHora.split(':').map(Number)
  const digestTotal = (dh || 0) * 60 + (dm || 0)

  if (madrid.hTotal < digestTotal || madrid.hTotal > digestTotal + 14) {
    console.log(
      `[cron/daily] Fuera de ventana ${digestHora} Madrid — ` +
      `hora actual Madrid: ${String(Math.floor(madrid.hTotal / 60)).padStart(2,'0')}:${String(madrid.hTotal % 60).padStart(2,'0')}`
    )
    return
  }

  // ── 2. Anti-duplicados por fecha en hora de Madrid ─────────────────────────
  const today      = madrid.date   // "YYYY-MM-DD" en zona horaria de Madrid
  const USUARIO_ID = 1

  const { data: logRows } = await supabaseRequest(
    env, 'GET',
    `push_log?usuario_id=eq.${USUARIO_ID}&tipo=eq.daily_summary&clave=eq.${today}&select=id`
  )
  if (Array.isArray(logRows) && logRows.length > 0) {
    console.log('[cron/daily] Resumen ya enviado hoy, omitiendo')
    return
  }

  // ── 3. Buscar tareas vencidas o de hoy ─────────────────────────────────────
  const weekAgo = new Date(Date.now() - 7 * 86_400_000).toISOString().split('T')[0]
  const params  = new URLSearchParams([
    ['esta_completada', 'eq.false'],
    ['fecha_limite',    `lte.${today}`],
    ['fecha_limite',    `gte.${weekAgo}`],
    ['usuario_id',      `eq.${USUARIO_ID}`],
    ['select',          'id,titulo,fecha_limite'],
    ['order',           'fecha_limite.asc'],
    ['limit',           '20'],
  ])
  const url = `${env.SUPABASE_URL}/rest/v1/tareas_table?${params.toString()}`
  const res  = await fetch(url, {
    headers: {
      apikey:        env.SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${env.SUPABASE_SERVICE_ROLE_KEY}`,
    },
  })
  const tareas = res.ok ? await res.json() : []

  if (!Array.isArray(tareas) || tareas.length === 0) {
    // No registrar en push_log: podría aparecer tarea más tarde en la misma ventana.
    console.log('[cron/daily] Sin tareas vencidas/hoy, no se envía resumen')
    return
  }

  // ── 4. Construir mensaje — "Hoy: X vencen, Y vencidas" ────────────────────
  const hoy      = tareas.filter(t => t.fecha_limite === today)
  const vencidas = tareas.filter(t => t.fecha_limite < today)

  const partes = []
  if (hoy.length > 0)      partes.push(`${hoy.length} vencen`)
  if (vencidas.length > 0) partes.push(`${vencidas.length} vencida${vencidas.length > 1 ? 's' : ''}`)

  const primeros = tareas.slice(0, 3).map(t => `"${t.titulo}"`).join(', ')
  const bodyText = tareas.length > 3 ? `${primeros}…` : primeros

  // ── 5. Enviar push ─────────────────────────────────────────────────────────
  const { enviadas } = await sendPushToUser(env, USUARIO_ID, {
    title: `📋 Hoy: ${partes.join(', ')}`,
    body:  bodyText,
    icon:  '/icons/icon-192.png',
    url:   '/',
    tag:   'daily-summary',
  })

  // Registrar SOLO si al menos una suscripción recibió la notificación
  if (enviadas > 0) {
    await supabaseRequest(env, 'POST', 'push_log', {
      usuario_id: USUARIO_ID,
      tipo:       'daily_summary',
      clave:      today,
    })
    console.log(`[cron/daily] Resumen enviado (${enviadas} suscripciones): ${partes.join(', ')}`)
  } else {
    console.log('[cron/daily] Sin suscripciones activas para enviar el resumen')
  }
}

// ── POST /api/push/test — notificación de prueba ──────────────────────────────

async function handlePushTest(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error
  const usuarioId = await resolveUser(auth.user.sub, env)
  if (usuarioId === null) return json({ error: 'Usuario no encontrado' }, 404)

  const { enviadas, intentadas } = await sendPushToUser(env, usuarioId, {
    title: 'MyTaskMyHabit',
    body:  'Notificación de prueba: todo funciona',
    icon:  '/icons/icon-192.png',
    tag:   'push-test',
  })
  return json({ data: { intentadas: intentadas ?? 0, enviadas: enviadas ?? 0 } })
}

// ── GET /api/push/public-key — clave pública VAPID ───────────────────────────

async function handlePushPublicKey(request, env) {
  const auth = await requireAuth(request, env)
  if (auth.error) return auth.error

  if (!env.VAPID_PUBLIC_KEY) {
    return json({ error: 'Notificaciones no configuradas en el servidor' }, 503)
  }
  return json({ data: { publicKey: env.VAPID_PUBLIC_KEY } })
}

// ── Router ────────────────────────────────────────────────────────────────────

async function dispatch(request, env) {
  const { pathname } = new URL(request.url)
  const method = request.method
  // Split path and drop empty segments: '/api/tareas/42' → ['api','tareas','42']
  const p = pathname.split('/').filter(Boolean)

  // Health
  if (pathname === '/health' && (method === 'GET' || method === 'HEAD')) return healthCheck(request, env)

  // Auth
  if (pathname === '/auth/login'           && method === 'POST') return handleLogin(request, env)
  if (pathname === '/auth/me'              && method === 'GET')  return handleMe(request, env)
  if (pathname === '/auth/realtime-token'  && method === 'GET')  return handleRealtimeToken(request, env)

  // Webhooks
  if (pathname === '/webhooks/email' && method === 'POST') return handleWebhookEmail(request, env)

  // Push subscriptions + test + public key
  if (pathname === '/api/push/subscribe'   && method === 'POST')   return handlePushSubscribe(request, env)
  if (pathname === '/api/push/subscribe'   && method === 'DELETE') return handlePushUnsubscribe(request, env)
  if (pathname === '/api/push/test'        && method === 'POST')   return handlePushTest(request, env)
  if (pathname === '/api/push/public-key'  && method === 'GET')    return handlePushPublicKey(request, env)

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
    // Adjuntos: /api/tareas/:id/adjuntos  y  /api/tareas/:id/adjuntos/:adjuntoId
    if (p[3]==='adjuntos') {
      if (p.length===4) {
        if (method === 'GET')  return handleGetAdjuntos(request, env, p[2])
        if (method === 'POST') return handlePostAdjunto(request, env, p[2])
      }
      if (p.length===5 && method === 'DELETE')
        return handleDeleteAdjunto(request, env, p[2], p[4])
    }
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
  async scheduled(_event, env, _ctx) {
    await handleScheduledDailySummary(env)
  },

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
