/**
 * Genera un par de claves VAPID (P-256 / ES256) para notificaciones push.
 *
 * Ejecutar UNA SOLA VEZ desde la raíz del proyecto:
 *   node task-worker/scripts/generate-vapid-keys.mjs
 *
 * Luego sigue las instrucciones que imprime en pantalla.
 * NUNCA subas las claves al repositorio.
 */

const keyPair = await crypto.subtle.generateKey(
  { name: 'ECDSA', namedCurve: 'P-256' },
  true,
  ['sign', 'verify']
)

// Clave privada como JWK (para el Worker secret VAPID_PRIVATE_KEY_JWK)
const privateJwk = await crypto.subtle.exportKey('jwk', keyPair.privateKey)

// Clave pública como raw 65 bytes uncompressed → base64url (para VAPID_PUBLIC_KEY y VITE_VAPID_PUBLIC_KEY)
const publicRaw = new Uint8Array(await crypto.subtle.exportKey('raw', keyPair.publicKey))
let binary = ''
for (const b of publicRaw) binary += String.fromCharCode(b)
const publicBase64url = btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')

console.log()
console.log('='.repeat(70))
console.log('  VAPID Keys generadas  —  NO subas estos valores al repo')
console.log('='.repeat(70))
console.log()
console.log('1) En task-worker/ ejecuta:')
console.log()
console.log('   npx wrangler secret put VAPID_PRIVATE_KEY_JWK')
console.log('   # Pega este valor exacto (en una sola línea):')
console.log()
console.log(JSON.stringify(privateJwk))
console.log()
console.log('   npx wrangler secret put VAPID_SUBJECT')
console.log('   # Pega: mailto:tu-email@dominio.com')
console.log()
console.log('2) En task-worker/wrangler.toml, en la sección [vars], añade/reemplaza:')
console.log()
console.log(`   VAPID_PUBLIC_KEY = "${publicBase64url}"`)
console.log()
console.log('3) En web/.env (local) y en Cloudflare Pages → Settings → Env vars, añade:')
console.log()
console.log(`   VITE_VAPID_PUBLIC_KEY="${publicBase64url}"`)
console.log()
console.log('='.repeat(70))
