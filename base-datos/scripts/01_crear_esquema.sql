-- =====================================================================
-- CaptaBot – Equipo ERROR 404
-- Script 01: esquema de la base de datos (PostgreSQL 16)
-- Historias relacionadas: SCRUM-36 (crear la base de datos), HU03, HU05
-- Ejecutar sobre una base vacía:  psql -d captabot -f 01_crear_esquema.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Usuarios del panel (equipo de ventas)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id               BIGSERIAL     PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL,
    correo           VARCHAR(150)  NOT NULL,
    contrasena_hash  VARCHAR(255)  NOT NULL,              -- BCrypt
    rol              VARCHAR(20)   NOT NULL DEFAULT 'VENDEDOR',
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR'))
);

-- ---------------------------------------------------------------------
-- Prospectos captados por el asistente virtual
-- ---------------------------------------------------------------------
CREATE TABLE prospectos (
    id                BIGSERIAL      PRIMARY KEY,
    nombre            VARCHAR(120)   NOT NULL,
    telefono          VARCHAR(9)     NOT NULL,
    servicio_interes  VARCHAR(120)   NOT NULL,
    presupuesto       NUMERIC(12, 2) NOT NULL,           -- en soles
    urgencia          VARCHAR(10)    NOT NULL,
    puntaje           SMALLINT       NOT NULL,
    calificacion      VARCHAR(10)    NOT NULL,
    etapa             VARCHAR(20)    NOT NULL DEFAULT 'NUEVO',
    canal             VARCHAR(30)    NOT NULL DEFAULT 'CHAT_WEB',
    vendedor_id       BIGINT         REFERENCES usuarios (id),
    creado_en         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    actualizado_en    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_prospectos_telefono     CHECK (telefono ~ '^[0-9]{9}$'),
    CONSTRAINT ck_prospectos_presupuesto  CHECK (presupuesto >= 0),
    CONSTRAINT ck_prospectos_urgencia     CHECK (urgencia IN ('ALTA', 'MEDIA', 'BAJA')),
    CONSTRAINT ck_prospectos_puntaje      CHECK (puntaje BETWEEN 0 AND 100),
    CONSTRAINT ck_prospectos_calificacion CHECK (calificacion IN ('CALIENTE', 'TIBIO', 'FRIO')),
    CONSTRAINT ck_prospectos_etapa        CHECK (etapa IN ('NUEVO', 'CONTACTADO', 'COTIZADO',
                                                           'CERRADO_GANADO', 'CERRADO_PERDIDO'))
);

CREATE INDEX ix_prospectos_calificacion ON prospectos (calificacion);
CREATE INDEX ix_prospectos_etapa        ON prospectos (etapa);
CREATE INDEX ix_prospectos_creado_en    ON prospectos (creado_en);

-- ---------------------------------------------------------------------
-- Historial de cambios de etapa (HU09)
-- ---------------------------------------------------------------------
CREATE TABLE historial_etapas (
    id              BIGSERIAL    PRIMARY KEY,
    prospecto_id    BIGINT       NOT NULL REFERENCES prospectos (id) ON DELETE CASCADE,
    etapa_anterior  VARCHAR(20),
    etapa_nueva     VARCHAR(20)  NOT NULL,
    usuario_id      BIGINT       REFERENCES usuarios (id),
    cambiado_en     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_historial_prospecto ON historial_etapas (prospecto_id);

-- ---------------------------------------------------------------------
-- Notas de seguimiento (HU10)
-- ---------------------------------------------------------------------
CREATE TABLE notas_seguimiento (
    id            BIGSERIAL    PRIMARY KEY,
    prospecto_id  BIGINT       NOT NULL REFERENCES prospectos (id) ON DELETE CASCADE,
    usuario_id    BIGINT       NOT NULL REFERENCES usuarios (id),
    contenido     TEXT         NOT NULL,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    editado_en    TIMESTAMPTZ
);

CREATE INDEX ix_notas_prospecto ON notas_seguimiento (prospecto_id);

-- ---------------------------------------------------------------------
-- Mantener actualizado_en al modificar un prospecto
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_tocar_actualizado_en() RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_prospectos_actualizado_en
    BEFORE UPDATE ON prospectos
    FOR EACH ROW EXECUTE FUNCTION fn_tocar_actualizado_en();

COMMIT;
