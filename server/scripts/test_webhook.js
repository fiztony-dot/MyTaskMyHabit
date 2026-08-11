#!/usr/bin/env node
/**
 * Test del webhook /webhooks/email
 *
 * Uso:
 *   node scripts/test_webhook.js [base_url]
 *
 * Ejemplos:
 *   node scripts/test_webhook.js                             # local
 *   node scripts/test_webhook.js https://mytaskmyhabit-api.onrender.com
 *
 * Variables de entorno:
 *   WEBHOOK_SECRET  — debe coincidir con la configurada en el servidor
 */

const BASE_URL = process.argv[2] || 'http://localhost:10000';
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || 'test-secret-local';

async function enviarWebhook({ subject, text, from, secret }) {
  const form = new FormData();
  form.append('subject', subject);
  form.append('text', text);
  form.append('from', from);

  const url = `${BASE_URL}/webhooks/email?secret=${encodeURIComponent(secret)}`;
  const res = await fetch(url, { method: 'POST', body: form });
  const data = await res.json();
  return { status: res.status, data };
}

async function main() {
  console.log(`\n🔗 Base URL: ${BASE_URL}`);
  console.log(`🔑 Secret: ${WEBHOOK_SECRET}\n`);

  // ── Test 1: secret incorrecto → 403 ──────────────────────────────────────
  console.log('── Test 1: secret incorrecto ──');
  const t1 = await enviarWebhook({
    subject: 'Test 1',
    text: 'Esto no debería crearse',
    from: 'tony@example.com',
    secret: 'secreto-invalido',
  });
  const ok1 = t1.status === 403;
  console.log(`  Status: ${t1.status} — ${ok1 ? '✅ 403 correcto' : '❌ Esperaba 403'}`);

  // ── Test 2: tarea normal ──────────────────────────────────────────────────
  console.log('\n── Test 2: tarea con asunto y cuerpo ──');
  const t2 = await enviarWebhook({
    subject: '[TEST] Revisar informe mensual',
    text: [
      'Necesito revisar el informe del proyecto antes del viernes.',
      '',
      'Incluye los apartados de presupuesto y seguimiento.',
      '',
      '--',
      'Tony',
    ].join('\n'),
    from: 'Tony <tony@example.com>',
    secret: WEBHOOK_SECRET,
  });
  const ok2 = t2.status === 200 && t2.data.ok === true;
  console.log(`  Status: ${t2.status} — ${ok2 ? `✅ Tarea id=${t2.data.tareaId}` : `❌ ${JSON.stringify(t2.data)}`}`);

  // ── Test 3: email con historial quoted ────────────────────────────────────
  console.log('\n── Test 3: cuerpo con historial quoted ──');
  const t3 = await enviarWebhook({
    subject: '[TEST] Tarea con historial en el cuerpo',
    text: [
      'Recuerda llamar al cliente.',
      '',
      'On Mon, 1 Jan 2025, Tony wrote:',
      '> ¿Tienes el número?',
      '> El cliente preguntó ayer.',
    ].join('\n'),
    from: 'tony@example.com',
    secret: WEBHOOK_SECRET,
  });
  const ok3 = t3.status === 200 && t3.data.ok === true;
  console.log(`  Status: ${t3.status} — ${ok3 ? `✅ Tarea id=${t3.data.tareaId}` : `❌ ${JSON.stringify(t3.data)}`}`);

  // ── Test 4: sin asunto ────────────────────────────────────────────────────
  console.log('\n── Test 4: sin asunto ──');
  const t4 = await enviarWebhook({
    subject: '',
    text: 'Tarea sin asunto de prueba.',
    from: 'tony@example.com',
    secret: WEBHOOK_SECRET,
  });
  const ok4 = t4.status === 200 && t4.data.ok === true;
  console.log(`  Status: ${t4.status} — ${ok4 ? `✅ Tarea id=${t4.data.tareaId}` : `❌ ${JSON.stringify(t4.data)}`}`);

  // ── Resumen ───────────────────────────────────────────────────────────────
  const total = [ok1, ok2, ok3, ok4].filter(Boolean).length;
  console.log(`\n════ Resultado: ${total}/4 tests OK ════\n`);
  process.exit(total === 4 ? 0 : 1);
}

main().catch((err) => {
  console.error('\n❌ Error inesperado:', err.message);
  process.exit(1);
});
