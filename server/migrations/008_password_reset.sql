-- Ejecutar en el proyecto clpowitmhoibiuszetpg (mytaskmyhabit)
--
-- Migración 008: Cambio y recuperación de contraseña
--
-- NO ejecutar automáticamente. Aplicar manualmente en el SQL Editor de Supabase
-- una sola vez (es idempotente: usa ADD COLUMN IF NOT EXISTS, CREATE TABLE IF NOT EXISTS).
--
-- Después de aplicar: configurar SENDGRID_API_KEY como secret del Worker y redesplegar.
-- Ver PROYECTO_PWA.md sección "Pasos manuales — Cambio y recuperación de contraseña".
--
-- Rollback:
--   ALTER TABLE usuarios DROP COLUMN IF EXISTS password_hash;
--   ALTER TABLE usuarios DROP COLUMN IF EXISTS password_changed_at;
--   ALTER TABLE usuarios DROP COLUMN IF EXISTS email;
--   DROP TABLE IF EXISTS auth_rate_limits;
--   DROP TABLE IF EXISTS password_resets;

-- ── 1. Columnas nuevas en usuarios ────────────────────────────────────────────
-- password_hash:        bcrypt hash de la contraseña guardada en BD (nullable hasta el
--                       primer cambio; mientras sea NULL, el Worker usa AUTH_USERS).
-- password_changed_at:  marca temporal del último cambio de contraseña; los JWT con
--                       iat anterior a esta fecha se rechazan con 401.
-- email:                dirección a la que se envía el enlace de recuperación.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS password_hash       TEXT;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email               TEXT;

-- Semilla: correo del usuario tony (solo si está vacío)
UPDATE usuarios SET email = 'tonyfiz@yahoo.es' WHERE id = 1 AND email IS NULL;

-- ── 2. Tabla password_resets ──────────────────────────────────────────────────
-- Una fila por intento de recuperación. token_hash almacena SHA-256 del token
-- (nunca el token en claro). La columna usada_en es NULL mientras el token no
-- se haya consumido.

CREATE TABLE IF NOT EXISTS password_resets (
  id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id  BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  token_hash  TEXT        NOT NULL UNIQUE,
  expira_en   TIMESTAMPTZ NOT NULL,
  usado_en    TIMESTAMPTZ,
  ip_address  TEXT,
  creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS password_resets_usuario_id_idx
  ON password_resets(usuario_id);

CREATE INDEX IF NOT EXISTS password_resets_token_hash_idx
  ON password_resets(token_hash);

-- ── 3. Tabla auth_rate_limits ─────────────────────────────────────────────────
-- Conteo de intentos fallidos por clave arbitraria (ej. "failed:tony",
-- "forgot_ip:1.2.3.4"). El Worker la usa para los límites de rate limiting
-- en change-password, reset-password y forgot-password.

CREATE TABLE IF NOT EXISTS auth_rate_limits (
  clave          TEXT        NOT NULL PRIMARY KEY,
  contador       INT         NOT NULL DEFAULT 0,
  ventana_inicio TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── 4. RLS — sin acceso directo de anon ni authenticated ─────────────────────
-- El Worker usa service_role key y bypassa RLS: no necesita políticas.
-- Los REVOKEs ya aplicados en usuarios siguen cubriendo las nuevas columnas
-- (password_hash, password_changed_at, email) sin cambio adicional, porque
-- el REVOKE es a nivel de tabla.

ALTER TABLE password_resets ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON password_resets FROM anon, authenticated;

ALTER TABLE auth_rate_limits ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON auth_rate_limits FROM anon, authenticated;

-- ── 5. Verificación ───────────────────────────────────────────────────────────
SELECT column_name, data_type, is_nullable
  FROM information_schema.columns
 WHERE table_schema = 'public' AND table_name = 'usuarios'
 ORDER BY ordinal_position;

SELECT tablename, rowsecurity
  FROM pg_tables
 WHERE schemaname = 'public'
 ORDER BY tablename;
