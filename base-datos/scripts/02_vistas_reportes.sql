-- =====================================================================
-- CaptaBot – Equipo ERROR 404
-- Script 02: vistas para los reportes (HU11 y HU12)
-- Ejecutar después de 01_crear_esquema.sql
-- =====================================================================

-- Prospectos captados por semana y por calificación (HU11)
CREATE OR REPLACE VIEW v_prospectos_por_semana AS
SELECT
    DATE_TRUNC('week', creado_en)::DATE  AS semana,
    COUNT(*)                             AS total,
    COUNT(*) FILTER (WHERE calificacion = 'CALIENTE') AS calientes,
    COUNT(*) FILTER (WHERE calificacion = 'TIBIO')    AS tibios,
    COUNT(*) FILTER (WHERE calificacion = 'FRIO')     AS frios
FROM prospectos
GROUP BY 1
ORDER BY 1;

-- Cantidad de prospectos en cada etapa del embudo (HU12)
CREATE OR REPLACE VIEW v_embudo_etapas AS
SELECT
    e.etapa,
    e.orden,
    COUNT(p.id) AS total
FROM (VALUES ('NUEVO', 1), ('CONTACTADO', 2), ('COTIZADO', 3),
             ('CERRADO_GANADO', 4), ('CERRADO_PERDIDO', 5)) AS e (etapa, orden)
LEFT JOIN prospectos p ON p.etapa = e.etapa
GROUP BY e.etapa, e.orden
ORDER BY e.orden;

-- Tasa de conversión: porcentaje de prospectos que terminan en CERRADO_GANADO (HU12)
CREATE OR REPLACE VIEW v_tasa_conversion AS
SELECT
    COUNT(*)                                          AS total_prospectos,
    COUNT(*) FILTER (WHERE etapa = 'CERRADO_GANADO')  AS ganados,
    ROUND(100.0 * COUNT(*) FILTER (WHERE etapa = 'CERRADO_GANADO')
          / NULLIF(COUNT(*), 0), 2)                   AS tasa_conversion_pct
FROM prospectos;
