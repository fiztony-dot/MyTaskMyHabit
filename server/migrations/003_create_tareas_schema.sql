-- Migración 003: Esquema del módulo Tareas
-- Crea el tipo ENUM de prioridad, la tabla de categorías y la tabla de tareas.
-- Corrección respecto a Room: categoria_id es FK formal (no string libre).

-- 1. Tipo ENUM para prioridad
DO $$ BEGIN
  CREATE TYPE prioridad_tarea AS ENUM ('ALTA', 'MEDIA', 'BAJA');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 2. Tabla de categorías de tareas
CREATE TABLE IF NOT EXISTS categorias_table (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo          TEXT NOT NULL,
    icono           TEXT NOT NULL DEFAULT 'list',
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
    activa          BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_categorias_usuario_id
    ON categorias_table (usuario_id);

-- 3. Tabla de tareas
CREATE TABLE IF NOT EXISTS tareas_table (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo              TEXT NOT NULL,
    descripcion         TEXT,
    esta_completada     BOOLEAN NOT NULL DEFAULT false,
    prioridad           prioridad_tarea NOT NULL DEFAULT 'MEDIA',
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_limite        DATE,
    hora_limite         TIME,
    categoria_id        BIGINT REFERENCES categorias_table(id) ON DELETE SET NULL,
    repeticion          TEXT NOT NULL DEFAULT 'Sin repetición',
    pendiente_clasificar BOOLEAN NOT NULL DEFAULT false,
    repeticion_fin      DATE,
    repeticion_veces    INTEGER,
    repeticion_contador INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tareas_usuario_id
    ON tareas_table (usuario_id);

CREATE INDEX IF NOT EXISTS idx_tareas_categoria_id
    ON tareas_table (categoria_id);

CREATE INDEX IF NOT EXISTS idx_tareas_orden_habitual
    ON tareas_table (usuario_id, prioridad, fecha_creacion);

CREATE INDEX IF NOT EXISTS idx_tareas_pendientes
    ON tareas_table (usuario_id, esta_completada)
    WHERE esta_completada = false;
