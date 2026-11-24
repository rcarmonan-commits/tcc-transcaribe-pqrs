-- =====================================================================
--  Datos de demostración. Se cargan ÚNICAMENTE con los perfiles
--  "dev" y "test" (ver spring.flyway.locations en application-*.yml).
--
--  Razón de seguridad: mantener credenciales conocidas fuera del camino
--  de migración de producción cierra el riesgo A05:2021 — Configuración
--  de seguridad incorrecta (cuentas por defecto desplegadas sin querer).
--
--  Contraseñas en claro, solo para el entorno local:
--    admin    -> Transc@ribe2026.Adm   (ROLE_ADMIN)
--    jlopez   -> Transc@ribe2026.Fun   (ROLE_FUNCIONARIO)
--    auditor  -> Transc@ribe2026.Aud   (ROLE_AUDITOR)
-- =====================================================================

-- Las tres cuentas son las de los integrantes del equipo, cada uno con el rol
-- que corresponde a la capa que trabajó. Son datos de demostración: el perfil
-- de producción no incluye esta ruta de migración.
INSERT INTO usuario (nombre_usuario, correo, hash_contrasena, nombres, apellidos, activo, intentos_fallidos, fecha_creacion) VALUES
  ('rcarmonan', 'rcarmonan@unicartagena.edu.co', '$2b$10$DV8NVi4YhQG2waslsnxy8OPSCRRpkg0bQCrtsqN2nbhvYaw6Y7p8W', 'Rosary Eilyn',   'Carmona Navarro', TRUE, 0, TIMESTAMP '2026-01-15 08:00:00'),
  ('jarnedoz',  'jarnedoz@unicartagena.edu.co',  '$2b$10$GJ/Buhkg9vZshfK.S4kAFujSEDVMixU.0XbNUfuCDMLVh8s18C2VK', 'José Luis',      'Arnedo Zambrano', TRUE, 0, TIMESTAMP '2026-01-15 08:05:00'),
  ('mortegas4', 'mortegas4@unicartagena.edu.co', '$2b$10$PiZ5McuRY5eNTA8gRmPh5Oybt6xz32T8Dd7Dk9uvHn6I9CMbAKSbS', 'Moisés Daniel',  'Ortega Sánchez',  TRUE, 0, TIMESTAMP '2026-01-15 08:10:00');

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u JOIN rol r ON r.codigo = 'ROLE_ADMIN'       WHERE u.nombre_usuario = 'rcarmonan';
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u JOIN rol r ON r.codigo = 'ROLE_FUNCIONARIO' WHERE u.nombre_usuario = 'rcarmonan';
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u JOIN rol r ON r.codigo = 'ROLE_FUNCIONARIO' WHERE u.nombre_usuario = 'jarnedoz';
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u JOIN rol r ON r.codigo = 'ROLE_AUDITOR'     WHERE u.nombre_usuario = 'mortegas4';

-- Rutas representativas del Sistema Integrado de Transporte Masivo.
INSERT INTO ruta (codigo, nombre, origen, destino, activa) VALUES
  ('T100', 'Troncal Portal – India Catalina',   'Portal Patio Portal',      'India Catalina',        TRUE),
  ('T101', 'Troncal Portal – Bodeguita',        'Portal Patio Portal',      'Bodeguita',             TRUE),
  ('T102', 'Troncal Madre Bernarda – Centro',   'Madre Bernarda',           'Centro',                TRUE),
  ('X104', 'Pretroncal Crespo – Centro',        'Crespo',                   'Centro',                TRUE),
  ('A101', 'Alimentador Villa Estrella',        'Portal Patio Portal',      'Villa Estrella',        TRUE),
  ('A102', 'Alimentador Nuevo Bosque',          'Portal Patio Portal',      'Nuevo Bosque',          TRUE),
  ('A103', 'Alimentador La Boquilla',           'Estación La Vía',          'La Boquilla',           TRUE);

INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXT101', '1101', t.id, 'OPERATIVO',         DATE '2024-03-01' FROM tipo_vehiculo t WHERE t.codigo = 'ARTICULADO';
INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXT102', '1102', t.id, 'OPERATIVO',         DATE '2024-03-01' FROM tipo_vehiculo t WHERE t.codigo = 'ARTICULADO';
INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXP201', '2201', t.id, 'OPERATIVO',         DATE '2024-06-15' FROM tipo_vehiculo t WHERE t.codigo = 'PADRON';
INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXP202', '2202', t.id, 'EN_MANTENIMIENTO',  DATE '2024-06-15' FROM tipo_vehiculo t WHERE t.codigo = 'PADRON';
INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXA301', '3301', t.id, 'OPERATIVO',         DATE '2025-01-20' FROM tipo_vehiculo t WHERE t.codigo = 'ALIMENTADOR';
INSERT INTO vehiculo (placa, numero_interno, tipo_vehiculo_id, estado_operativo, fecha_vinculacion)
SELECT 'UXA302', '3302', t.id, 'FUERA_DE_SERVICIO', DATE '2025-01-20' FROM tipo_vehiculo t WHERE t.codigo = 'ALIMENTADOR';

INSERT INTO ciudadano (tipo_documento_id, numero_documento, nombres, apellidos, correo, telefono, fecha_registro)
SELECT td.id, '1047398221', 'Carlos Andrés', 'Pacheco', 'capacheco@correo.com', '3015558841', TIMESTAMP '2026-02-02 09:12:00'
FROM tipo_documento td WHERE td.codigo = 'CC';
INSERT INTO ciudadano (tipo_documento_id, numero_documento, nombres, apellidos, correo, telefono, fecha_registro)
SELECT td.id, '45512903', 'Yulieth', 'Marrugo', 'ymarrugo@correo.com', '3204417762', TIMESTAMP '2026-02-03 14:40:00'
FROM tipo_documento td WHERE td.codigo = 'CC';
