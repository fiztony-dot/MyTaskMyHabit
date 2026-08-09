-- Migración 002: Seed del usuario único
-- Inserta el usuario real de la aplicación. Idempotente: si ya existe, no hace nada.

INSERT INTO usuarios (username, nombre_visible)
VALUES ('tony', 'Tony')
ON CONFLICT (username) DO NOTHING;
