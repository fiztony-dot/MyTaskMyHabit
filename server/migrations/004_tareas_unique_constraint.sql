-- Migración 004: Constraint UNIQUE para idempotencia de la migración de datos
-- Permite re-ejecutar el script de migración sin duplicar filas.

-- Categorías: (usuario_id, titulo) debe ser única
CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_usuario_titulo
    ON categorias_table (usuario_id, titulo);

-- Tareas: (usuario_id, titulo, fecha_creacion) como clave natural
-- Nota: fecha_creacion incluye milisegundos, así que dos tareas con el mismo
-- título creadas en momentos diferentes son entradas distintas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tareas_usuario_titulo_fecha
    ON tareas_table (usuario_id, titulo, fecha_creacion);
