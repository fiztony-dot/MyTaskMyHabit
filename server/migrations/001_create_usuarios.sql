-- Migración 001: Crear tabla usuarios
-- Tabla de referencia para FKs en todas las tablas de negocio.
-- NO gestiona contraseñas (eso lo resuelve AUTH_USERS + JWT).

CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        TEXT NOT NULL UNIQUE,
    nombre_visible  TEXT,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);
