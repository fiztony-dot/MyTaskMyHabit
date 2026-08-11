import PostalMime from 'postal-mime';

const BACKEND_URL = 'https://mytaskmyhabit-api.onrender.com/webhooks/email';

// ── Limpieza del cuerpo ───────────────────────────────────────────────────────

function limpiarCuerpo(texto) {
  if (!texto) return null;

  const lineas = texto.split('\n');
  const resultado = [];

  for (const linea of lineas) {
    const trim = linea.trim();

    // Firma estándar: -- / — / --- solos en una línea
    if (trim === '--' || trim === '—' || trim === '---') break;

    // Historia quoted: líneas que empiezan por >
    if (trim.startsWith('>')) continue;

    // Cabecera de bloque quoted: "On ... wrote:" / "El ... escribió:"
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

// ── Handler principal ─────────────────────────────────────────────────────────

export default {
  async email(message, env, ctx) {
    try {
      // Bufferar el stream (ReadableStream es single-use)
      const buffer = await new Response(message.raw).arrayBuffer();
      const parsed = await PostalMime.parse(buffer);

      // Asunto: postal-mime lo decodifica correctamente (UTF-8, Q-encoding, etc.)
      const subject = (parsed.subject || 'Sin asunto').substring(0, 255);

      // Remitente: usar el envelope from (SMTP MAIL FROM, más seguro que el header)
      const from = message.from;

      // Cuerpo: preferir texto plano; fallback a HTML con etiquetas eliminadas
      let bodyText = parsed.text;
      if (!bodyText && parsed.html) {
        bodyText = stripHtml(parsed.html);
      }
      const textoLimpio = limpiarCuerpo(bodyText) ?? '';

      console.log(`[email-worker] from=${from} subject="${subject}" body_len=${textoLimpio.length}`);

      // Llamar al backend de Render con multipart/form-data
      const form = new FormData();
      form.append('subject', subject);
      form.append('text', textoLimpio);
      form.append('from', from);

      const secret = env.WEBHOOK_SECRET || '';
      const url = `${BACKEND_URL}?secret=${encodeURIComponent(secret)}`;

      const res = await fetch(url, { method: 'POST', body: form });
      const data = await res.json().catch(() => ({}));
      console.log(`[email-worker] webhook status=${res.status} ok=${data.ok} tareaId=${data.tareaId}`);

    } catch (err) {
      // No relanzar — el worker no debe fallar ni rechazar el email
      console.error('[email-worker] error:', err.message);
    }
  },
};
