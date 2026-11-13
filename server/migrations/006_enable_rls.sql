-- Migración 006: Activar Row Level Security (RLS) en todas las tablas del esquema public
--
-- NO ejecutar automáticamente. Aplicar manualmente en el SQL Editor de Supabase
-- una vez que el Worker con /auth/realtime-token esté desplegado y verificado.
--
-- Arquitectura:
--   · Worker  → usa SUPABASE_SERVICE_ROLE_KEY → bypasses RLS → sin cambios
--   · Android → solo habla con el Worker, nunca con Supabase → sin cambios
--   · PWA     → anon key solo para Realtime → token autenticado via /auth/realtime-token
--
-- Orden de despliegue obligatorio (ver PROYECTO_PWA.md sección S1):
--   1. Configurar SUPABASE_JWT_SECRET en el Worker
--   2. Desplegar Worker y PWA
--   3. Verificar Realtime con el token nuevo
--   4. Aplicar ESTE script
--
-- Rollback (si algo falla después de aplicar):
--   ALTER TABLE usuarios         DISABLE ROW LEVEL SECURITY;
--   ALTER TABLE categorias_table DISABLE ROW LEVEL SECURITY;
--   ALTER TABLE tareas_table     DISABLE ROW LEVEL SECURITY;
--   ALTER TABLE tarea_adjuntos   DISABLE ROW LEVEL SECURITY;
--   ALTER TABLE migration_log    DISABLE ROW LEVEL SECURITY;

-- ── 1. Activar RLS en todas las tablas de public ─────────────────────────────

ALTER TABLE migration_log    ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios         ENABLE ROW LEVEL SECURITY;
ALTER TABLE categorias_table ENABLE ROW LEVEL SECURITY;
ALTER TABLE tareas_table     ENABLE ROW LEVEL SECURITY;
ALTER TABLE tarea_adjuntos   ENABLE ROW LEVEL SECURITY;

-- ── 2. Revocar acceso directo de anon y authenticated ───────────────────────
-- service_role bypassa RLS; estos REVOKEs solo afectan a anon/authenticated.
-- Capa de defensa en profundidad: aunque RLS se desactivara accidentalmente,
-- anon no podría leer los datos.

REVOKE ALL ON migration_log    FROM anon, authenticated;
REVOKE ALL ON usuarios         FROM anon, authenticated;
REVOKE ALL ON categorias_table FROM anon, authenticated;
REVOKE ALL ON tarea_adjuntos   FROM anon, authenticated;

-- tareas_table: anon pierde todo; authenticated conserva solo SELECT (para Realtime)
REVOKE ALL ON tareas_table FROM anon;
REVOKE INSERT, UPDATE, DELETE ON tareas_table FROM authenticated;

-- ── 3. Política RLS para Realtime en tareas_table ────────────────────────────
-- SELECT permitido solo al rol 'authenticated' cuando el sub del JWT (usuario_id
-- numérico como string) coincide con el usuario_id de la fila.
-- El token lo genera el Worker en GET /auth/realtime-token con sub=String(usuario_id).

DROP POLICY IF EXISTS "realtime_select_own" ON tareas_table;
CREATE POLICY "realtime_select_own" ON tareas_table
  FOR SELECT
  TO authenticated
  USING (usuario_id::text = (auth.jwt() ->> 'sub'));

-- ── 4. Verificación (resultados esperados) ───────────────────────────────────
-- rowsecurity = true en todas las tablas de public
-- política "realtime_select_own" en tareas_table con cmd=SELECT, roles={authenticated}

SELECT tablename, rowsecurity
  FROM pg_tables
 WHERE schemaname = 'public'
 ORDER BY tablename;

SELECT schemaname, tablename, policyname, cmd, roles, qual
  FROM pg_policies
 WHERE schemaname = 'public'
 ORDER BY tablename, policyname;
