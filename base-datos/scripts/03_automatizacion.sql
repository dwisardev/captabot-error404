-- =============================================================================
-- CaptaBot – Automatización (HU13): avisos por Telegram y seguimiento
-- Se ejecuta después de 01_crear_esquema.sql y 02_vistas_reportes.sql.
-- Se puede volver a ejecutar sin errores en una base que ya existe.
-- =============================================================================

-- Chats de Telegram del equipo de ventas que reciben los avisos.
-- Un vendedor se agrega escribiendo al bot: /vendedor <código del .env>
CREATE TABLE IF NOT EXISTS avisos_telegram (
    chat_id    BIGINT       PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL,
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE avisos_telegram IS 'Chats de Telegram del equipo de ventas que reciben los avisos de prospectos (HU13)';

-- Datos del prospecto que usa la automatización
ALTER TABLE prospectos ADD COLUMN IF NOT EXISTS telegram_chat_id       BIGINT;
ALTER TABLE prospectos ADD COLUMN IF NOT EXISTS recordatorio_en        TIMESTAMPTZ;
ALTER TABLE prospectos ADD COLUMN IF NOT EXISTS seguimiento_cliente_en TIMESTAMPTZ;

COMMENT ON COLUMN prospectos.telegram_chat_id IS 'Chat de Telegram del cliente, si llegó por el bot (para el seguimiento)';
COMMENT ON COLUMN prospectos.recordatorio_en IS 'Cuándo se recordó al equipo que el prospecto seguía sin contactar';
COMMENT ON COLUMN prospectos.seguimiento_cliente_en IS 'Cuándo el bot envió el mensaje de seguimiento al cliente';

-- Búsqueda de pendientes que hace la API cada minuto
CREATE INDEX IF NOT EXISTS ix_prospectos_pendientes_aviso
    ON prospectos (etapa, creado_en)
    WHERE recordatorio_en IS NULL OR seguimiento_cliente_en IS NULL;
