-- Ejecutar en el proyecto clpowitmhoibiuszetpg (mytaskmyhabit)
--
-- Migración 007: Tablas para notificaciones push (Web Push / VAPID)
--
-- NO ejecutar automáticamente. Aplicar manualmente en el SQL Editor de Supabase
-- una sola vez (es idempotente: usa CREATE TABLE IF NOT EXISTS).
--
-- Después de aplicar: configurar secrets VAPID en el Worker y desplegar.
-- Ver PROYECTO_PWA.md sección "Pasos manuales — Web Push / VAPID".
--
-- Rollback:
--   DROP TABLE IF EXISTS push_log;
--   DROP TABLE IF EXISTS push_subscriptions;

-- ── 1. Suscripciones push (una fila por dispositivo/navegador) ────────────────

CREATE TABLE IF NOT EXISTS push_subscriptions (
  id              BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id      BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  endpoint        TEXT        NOT NULL UNIQUE,
  p256dh          TEXT        NOT NULL,
  auth            TEXT        NOT NULL,
  user_agent      TEXT,
  creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
  ultimo_envio_ok TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS push_subscriptions_usuario_id_idx
  ON push_subscriptions(usuario_id);

-- ── 2. Log de notificaciones enviadas ────────────────────────────────────────
-- Permite deduplicar el resumen diario: si ya existe una fila con
-- tipo='daily_summary' y clave='YYYY-MM-DD' para el usuario, no se reenvía.
-- También registra los avisos de tarea nueva por email.

CREATE TABLE IF NOT EXISTS push_log (
  id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id  BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo        TEXT        NOT NULL,
  clave       TEXT        NOT NULL,
  enviado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice para búsqueda de deduplicación: usuario + tipo + clave
CREATE INDEX IF NOT EXISTS push_log_dedup_idx
  ON push_log(usuario_id, tipo, clave);

-- ── 3. RLS — sin acceso directo de anon ni authenticated ─────────────────────
-- El Worker usa service_role key y bypassa RLS: no necesita políticas.
-- Defensa en profundidad: aunque RLS se desactivara, anon/authenticated no llegan.

ALTER TABLE push_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE push_log           ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON push_subscriptions FROM anon, authenticated;
REVOKE ALL ON push_log           FROM anon, authenticated;

-- ── 4. Verificación ───────────────────────────────────────────────────────────
SELECT tablename, rowsecurity
  FROM pg_tables
 WHERE schemaname = 'public'
 ORDER BY tablename;
