const express = require('express');
const multer = require('multer');
const sgMail = require('@sendgrid/mail');
const { pool } = require('../db');

const router = express.Router();
const upload = multer();

if (process.env.SENDGRID_API_KEY) {
  sgMail.setApiKey(process.env.SENDGRID_API_KEY);
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function verificarSecret(req, res, next) {
  const secret = process.env.WEBHOOK_SECRET;
  if (!secret || req.query.secret !== secret) {
    return res.status(403).json({ error: 'Forbidden' });
  }
  next();
}

function extraerEmail(fromStr) {
  if (!fromStr) return null;
  const match = fromStr.match(/<([^>]+)>/);
  return match ? match[1].trim() : fromStr.trim();
}

function limpiarCuerpo(texto) {
  if (!texto) return null;

  const lineas = texto.split('\n');
  const resultado = [];

  for (const linea of lineas) {
    const trim = linea.trim();

    // Firma estándar: -- o — o --- solos en una línea
    if (trim === '--' || trim === '—' || trim === '---') break;

    // Historia quoted: línea empieza por >
    if (trim.startsWith('>')) continue;

    // Cabecera de bloque quoted: "On ... wrote:" / "El ... escribió:"
    if (/^(On\s|El\s).+wrote\s*:/i.test(trim) || /^(On\s|El\s).+escribi[oó]\s*:/i.test(trim)) break;

    resultado.push(linea);
  }

  const limpio = resultado.join('\n').trim();
  if (!limpio) return null;
  return limpio.length > 1000 ? limpio.substring(0, 997) + '...' : limpio;
}

// ── Webhook POST /webhooks/email ─────────────────────────────────────────────

router.post('/email', verificarSecret, upload.none(), async (req, res) => {
  try {
    const { subject, text, from } = req.body || {};

    const titulo = (subject || 'Sin asunto').substring(0, 255);
    const descripcion = limpiarCuerpo(text);
    const emailRemitente = extraerEmail(from);

    const result = await pool.query(
      `INSERT INTO tareas_table
         (titulo, descripcion, usuario_id, categoria_id, pendiente_clasificar,
          prioridad, esta_completada, fecha_creacion)
       VALUES ($1, $2, 1, NULL, true, 'MEDIA', false, NOW())
       RETURNING id`,
      [titulo, descripcion]
    );
    const tareaId = result.rows[0].id;
    console.log(`[webhook/email] Tarea id=${tareaId} creada: "${titulo}"`);

    // Enviar confirmación — fallo no debe romper el webhook
    if (emailRemitente && process.env.SENDGRID_API_KEY && process.env.SENDGRID_FROM_EMAIL) {
      try {
        await sgMail.send({
          to: emailRemitente,
          from: process.env.SENDGRID_FROM_EMAIL,
          subject: `✅ Tarea creada: ${titulo}`,
          text: [
            `Tu tarea ha sido creada en MyTaskMyHabit:`,
            ``,
            `📋 ${titulo}`,
            ``,
            `Está pendiente de clasificar. Ábrela en la app para asignarle fecha, categoría y prioridad.`,
            ``,
            `— MyTaskMyHabit`,
          ].join('\n'),
        });
        console.log(`[webhook/email] Confirmación enviada a ${emailRemitente}`);
      } catch (emailErr) {
        console.error('[webhook/email] Error enviando confirmación:', emailErr.message);
      }
    } else {
      console.log('[webhook/email] Confirmación omitida (SENDGRID_API_KEY o FROM_EMAIL no configurados)');
    }

    // SendGrid requiere 200 para no reintentar
    res.status(200).json({ ok: true, tareaId });
  } catch (err) {
    console.error('[webhook/email] Error procesando webhook:', err);
    // 200 igualmente para evitar reintentos de SendGrid
    res.status(200).json({ ok: false, error: 'Internal error logged' });
  }
});

module.exports = router;
