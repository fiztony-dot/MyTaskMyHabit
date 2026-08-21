import PostalMime from 'postal-mime';

const BACKEND_URL = 'https://mytaskmyhabit-production.up.railway.app/webhooks/email';
const FETCH_TIMEOUT_MS = 30_000;

// ── Helpers ───────────────────────────────────────────────────────────────────

function limpiarCuerpo(texto) {
  if (!texto) return null;
  const lineas = texto.split('\n');
  const resultado = [];
  for (const linea of lineas) {
    const trim = linea.trim();
    if (trim === '--' || trim === '—' || trim === '---') break;
    if (trim.startsWith('>')) continue;
    if (
      /^(On\s|El\s).+wrote\s*:/i.test(trim) ||
      /^(On\s|El\s).+escribi[oó]\s*:/i.test(trim)
    ) break;
    resultado.push(linea);
  }
  const limpio = resultado.join('\n').trim();
  if (!limpio) return null;
  return limpio.length > 1000 ? limpio.substring(0, 997) + '...' : limpio;
}

function stripHtml(html) {
  if (!html) return null;
  const texto = html
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/\s{2,}/g, ' ')
    .trim();
  return texto || null;
}

// ── Fetch con timeout explícito ───────────────────────────────────────────────

async function fetchConTimeout(url, options) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } catch (err) {
    if (err.name === 'AbortError') throw new Error(`fetch timeout (>${FETCH_TIMEOUT_MS}ms)`);
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

// ── Handler principal ─────────────────────────────────────────────────────────

export default {
  async email(message, env, ctx) {
    // ctx.waitUntil() garantiza que el Worker no se corta antes de terminar
    // el fetch, incluso si la respuesta de Render tarda (free tier duerme).
    ctx.waitUntil((async () => {
      try {
        console.log(`[email-worker] START from=${message.from} to=${message.to} size=${message.rawSize}`);

        // 1. Verificar que el secret está configurado
        const secret = env.WEBHOOK_SECRET || '';
        if (!secret) console.warn('[email-worker] WARN: WEBHOOK_SECRET está vacío');

        // 2. Parsear MIME (bufferar primero — el stream es single-use)
        console.log('[email-worker] parsing MIME...');
        const buffer = await new Response(message.raw).arrayBuffer();
        const parsed = await PostalMime.parse(buffer);
        console.log(`[email-worker] parsed ok — subject="${parsed.subject}" hasText=${!!parsed.text} hasHtml=${!!parsed.html}`);

        // 3. Extraer campos
        const subject = (parsed.subject || 'Sin asunto').substring(0, 255);
        const from = message.from;

        let bodyText = parsed.text;
        if (!bodyText && parsed.html) {
          bodyText = stripHtml(parsed.html);
          console.log('[email-worker] using HTML→text fallback');
        }
        const textoLimpio = limpiarCuerpo(bodyText) ?? '';
        console.log(`[email-worker] body cleaned len=${textoLimpio.length}`);

        // 4. Construir FormData y llamar al backend
        const form = new FormData();
        form.append('subject', subject);
        form.append('text', textoLimpio);
        form.append('from', from);

        const url = `${BACKEND_URL}?secret=${encodeURIComponent(secret)}`;
        console.log(`[email-worker] fetching ${BACKEND_URL} (timeout=${FETCH_TIMEOUT_MS}ms)...`);

        const res = await fetchConTimeout(url, { method: 'POST', body: form });
        const data = await res.json().catch(() => ({}));
        console.log(`[email-worker] DONE status=${res.status} ok=${data.ok} tareaId=${data.tareaId}`);

      } catch (err) {
        console.error(`[email-worker] ERROR ${err.name}: ${err.message}`);
      }
    })());
  },
};
