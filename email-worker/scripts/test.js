#!/usr/bin/env node
/**
 * Prueba del flujo completo: llama al endpoint del backend como lo haría el Email Worker.
 *
 * Uso:
 *   WEBHOOK_SECRET=xxx node scripts/test.js [base_url]
 *
 * Ejemplos:
 *   WEBHOOK_SECRET=xxx node scripts/test.js
 *   WEBHOOK_SECRET=xxx node scripts/test.js https://mytaskmyhabit-api.onrender.com
 */

const BASE_URL = process.argv[2] || 'https://mytaskmyhabit-api.onrender.com';
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET;

if (!WEBHOOK_SECRET) {
  console.error('❌ Falta la variable de entorno WEBHOOK_SECRET');
  process.exit(1);
}

async function llamarWebhook({ subject, text, from }) {
  const form = new FormData();
  form.append('subject', subject);
  form.append('text', text);
  form.append('from', from);

  const url = `${BASE_URL}/webhooks/email?secret=${encodeURIComponent(WEBHOOK_SECRET)}`;
  const res = await fetch(url, { method: 'POST', body: form });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

async function main() {
  console.log(`\n🔗 Backend: ${BASE_URL}\n`);

  // ── Test 1: tarea normal con firma ────────────────────────────────────────
  console.log('── Test 1: tarea con asunto, cuerpo y firma ──');
  const t1 = await llamarWebhook({
    subject: '[EMAIL-WORKER TEST] Revisar informe mensual',
    text: [
      'Necesito revisar el informe antes del viernes.',
      '',
      'Incluye los apartados de presupuesto.',
      '',
      '--',
      'Tony',
    ].join('\n'),
    from: 'Tony <tony@example.com>',
  });
  const ok1 = t1.status === 200 && t1.data.ok === true;
  console.log(`  ${ok1 ? '✅' : '❌'} Status ${t1.status} — tareaId=${t1.data.tareaId ?? 'n/a'}`);

  // ── Test 2: cuerpo con historial quoted ───────────────────────────────────
  console.log('\n── Test 2: cuerpo con historial quoted ──');
  const t2 = await llamarWebhook({
    subject: '[EMAIL-WORKER TEST] Llamar al cliente',
    text: [
      'Recuerda llamar al cliente mañana.',
      '',
      'On Mon, 1 Jan 2025, Tony wrote:',
      '> ¿Tienes el número?',
    ].join('\n'),
    from: 'tony@example.com',
  });
  const ok2 = t2.status === 200 && t2.data.ok === true;
  console.log(`  ${ok2 ? '✅' : '❌'} Status ${t2.status} — tareaId=${t2.data.tareaId ?? 'n/a'}`);

  // ── Test 3: sin asunto ────────────────────────────────────────────────────
  console.log('\n── Test 3: sin asunto ──');
  const t3 = await llamarWebhook({
    subject: '',
    text: 'Tarea sin asunto.',
    from: 'tony@example.com',
  });
  const ok3 = t3.status === 200 && t3.data.ok === true;
  console.log(`  ${ok3 ? '✅' : '❌'} Status ${t3.status} — tareaId=${t3.data.tareaId ?? 'n/a'}`);

  // ── Resumen ───────────────────────────────────────────────────────────────
  const total = [ok1, ok2, ok3].filter(Boolean).length;
  console.log(`\n════ Resultado: ${total}/3 tests OK ════\n`);
  process.exit(total === 3 ? 0 : 1);
}

main().catch((err) => {
  console.error('\n❌ Error:', err.message);
  process.exit(1);
});
