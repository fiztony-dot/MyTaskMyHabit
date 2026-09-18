-- Migración 005: Tabla de adjuntos de tareas (Fase 1 — Adjuntos)
-- Almacena metadatos de ficheros (imágenes y documentos) subidos a una tarea.
-- Los ficheros físicos viven en Supabase Storage (bucket privado `tarea-adjuntos`);
-- esta tabla solo guarda la referencia (storage_path) y la URL pública/firmada.
-- FK CASCADE: al borrar la tarea o el usuario, sus adjuntos se eliminan de la BD.

CREATE TABLE IF NOT EXISTS tarea_adjuntos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tarea_id        BIGINT NOT NULL REFERENCES tareas_table(id) ON DELETE CASCADE,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre_fichero  TEXT NOT NULL,
    tipo_mime       TEXT NOT NULL,
    tamaño_bytes    BIGINT NOT NULL,
    storage_path    TEXT NOT NULL,
    url_publica     TEXT NOT NULL,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tarea_adjuntos_tarea
    ON tarea_adjuntos (tarea_id);
