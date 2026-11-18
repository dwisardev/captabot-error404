-- =====================================================================
-- CaptaBot – Equipo ERROR 404
-- Datos de prueba para desarrollo y demo (SCRUM-43)
-- Ejecutar después de los scripts de base-datos/scripts/
-- Contraseña de los dos usuarios de prueba: Demo2026!  (solo para desarrollo)
-- Las calificaciones siguen las reglas de la API (ver api/README.md).
-- =====================================================================

BEGIN;

INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES
    ('Administrador Demo', 'admin@captabot.pe',
     '$2a$10$a1lkdxu.fJvDokQ211vQ7uvUalSeiN2a6O/fv9fsDZZ.KqYTVQqWK', 'ADMINISTRADOR'),
    ('Vendedor Demo', 'ventas@captabot.pe',
     '$2a$10$a1lkdxu.fJvDokQ211vQ7uvUalSeiN2a6O/fv9fsDZZ.KqYTVQqWK', 'VENDEDOR');

-- puntaje = puntos por presupuesto + puntos por urgencia
INSERT INTO prospectos
    (nombre, telefono, servicio_interes, presupuesto, urgencia, puntaje, calificacion, etapa, vendedor_id, creado_en)
VALUES
    ('María Quispe',     '987654321', 'Diseño de página web',      6000.00, 'ALTA',  100, 'CALIENTE', 'COTIZADO',        2, NOW() - INTERVAL '20 days'),
    ('Jorge Ramírez',    '912345678', 'Mantenimiento de equipos',  2500.00, 'MEDIA',  65, 'TIBIO',    'CONTACTADO',      2, NOW() - INTERVAL '18 days'),
    ('Lucía Fernández',  '956781234', 'Marketing en redes',         300.00, 'BAJA',   15, 'FRIO',     'NUEVO',        NULL, NOW() - INTERVAL '15 days'),
    ('Carlos Mendoza',   '943218765', 'Diseño de página web',      1000.00, 'ALTA',   70, 'CALIENTE', 'CERRADO_GANADO',  2, NOW() - INTERVAL '12 days'),
    ('Ana Torres',       '965432187', 'Capacitación en Excel',      500.00, 'MEDIA',  50, 'TIBIO',    'CERRADO_PERDIDO', 2, NOW() - INTERVAL '10 days'),
    ('Pedro Castillo',   '921876543', 'Marketing en redes',        5500.00, 'MEDIA',  80, 'CALIENTE', 'CONTACTADO',      2, NOW() - INTERVAL '7 days'),
    ('Rosa Huamán',      '934567812', 'Mantenimiento de equipos',     0.00, 'MEDIA',  35, 'FRIO',     'NUEVO',        NULL, NOW() - INTERVAL '5 days'),
    ('Luis Paredes',     '978123456', 'Diseño de página web',      2000.00, 'BAJA',   45, 'TIBIO',    'NUEVO',        NULL, NOW() - INTERVAL '3 days'),
    ('Sofía Vargas',     '989012345', 'Capacitación en Excel',     3000.00, 'ALTA',   85, 'CALIENTE', 'NUEVO',        NULL, NOW() - INTERVAL '1 day'),
    ('Diego Salazar',    '917654320', 'Marketing en redes',         800.00, 'BAJA',   30, 'FRIO',     'NUEVO',        NULL, NOW());

INSERT INTO historial_etapas (prospecto_id, etapa_anterior, etapa_nueva, usuario_id, cambiado_en) VALUES
    (1, 'NUEVO',      'CONTACTADO',      2, NOW() - INTERVAL '19 days'),
    (1, 'CONTACTADO', 'COTIZADO',        2, NOW() - INTERVAL '17 days'),
    (2, 'NUEVO',      'CONTACTADO',      2, NOW() - INTERVAL '17 days'),
    (4, 'NUEVO',      'CONTACTADO',      2, NOW() - INTERVAL '11 days'),
    (4, 'CONTACTADO', 'COTIZADO',        2, NOW() - INTERVAL '10 days'),
    (4, 'COTIZADO',   'CERRADO_GANADO',  2, NOW() - INTERVAL '8 days'),
    (5, 'NUEVO',      'CONTACTADO',      2, NOW() - INTERVAL '9 days'),
    (5, 'CONTACTADO', 'CERRADO_PERDIDO', 2, NOW() - INTERVAL '6 days'),
    (6, 'NUEVO',      'CONTACTADO',      2, NOW() - INTERVAL '6 days');

INSERT INTO notas_seguimiento (prospecto_id, usuario_id, contenido, creado_en) VALUES
    (1, 2, 'Llamada inicial: necesita la web antes de fin de mes.',            NOW() - INTERVAL '19 days'),
    (1, 2, 'Se envió la cotización por correo.',                               NOW() - INTERVAL '17 days'),
    (4, 2, 'Aceptó la propuesta y pagó el adelanto.',                          NOW() - INTERVAL '8 days'),
    (5, 2, 'Eligió otro proveedor por precio.',                                NOW() - INTERVAL '6 days'),
    (6, 2, 'Pide una reunión el lunes para ver el plan de redes.',             NOW() - INTERVAL '5 days');

COMMIT;
