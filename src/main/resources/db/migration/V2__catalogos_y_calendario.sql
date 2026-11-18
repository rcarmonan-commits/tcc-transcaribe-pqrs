-- =====================================================================
--  Datos maestros del servicio de PQRS.
--  Son datos de operación, no de demostración: sin ellos el sistema no
--  puede radicar una solicitud ni calcular un plazo legal.
--  Los usuarios de prueba viven en db/demo y NO se cargan en producción.
-- =====================================================================

INSERT INTO tipo_documento (codigo, nombre) VALUES
  ('CC',  'Cédula de ciudadanía'),
  ('CE',  'Cédula de extranjería'),
  ('TI',  'Tarjeta de identidad'),
  ('PAS', 'Pasaporte'),
  ('NIT', 'Número de identificación tributaria');

-- Plazos tomados de la Ley 1755 de 2015, artículo 14.
INSERT INTO tipo_solicitud (codigo, nombre, dias_habiles_respuesta, fundamento_legal) VALUES
  ('PETICION',             'Petición de interés general o particular', 15, 'Ley 1755 de 2015, art. 14'),
  ('QUEJA',                'Queja',                                    15, 'Ley 1755 de 2015, art. 14'),
  ('RECLAMO',              'Reclamo',                                  15, 'Ley 1755 de 2015, art. 14'),
  ('SUGERENCIA',           'Sugerencia',                               15, 'Ley 1755 de 2015, art. 14'),
  ('DENUNCIA',             'Denuncia',                                 15, 'Ley 1755 de 2015, art. 14'),
  ('PETICION_DOCUMENTOS',  'Petición de documentos e información',     10, 'Ley 1755 de 2015, art. 14, num. 1'),
  ('CONSULTA',             'Consulta',                                 30, 'Ley 1755 de 2015, art. 14, num. 2');

INSERT INTO canal_recepcion (codigo, nombre) VALUES
  ('WEB',         'Portal web institucional'),
  ('APP_MOVIL',   'Aplicación móvil'),
  ('PRESENCIAL',  'Punto de atención presencial'),
  ('TELEFONICO',  'Línea telefónica de atención'),
  ('REDES',       'Redes sociales'),
  ('BUZON',       'Buzón físico en estación');

INSERT INTO categoria_pqrs (codigo, nombre, prioridad_base, activa) VALUES
  ('SERVICIO_RUTA',       'Frecuencia y cumplimiento de ruta',          'MEDIA',   TRUE),
  ('ESTADO_VEHICULO',     'Estado y aseo del vehículo',                 'MEDIA',   TRUE),
  ('TRATO_PERSONAL',      'Trato del personal de operación',            'ALTA',    TRUE),
  ('SEGURIDAD',           'Seguridad del usuario a bordo o en estación','CRITICA', TRUE),
  ('ACCIDENTE',           'Accidente o siniestro vial',                 'CRITICA', TRUE),
  ('TARIFA_RECAUDO',      'Tarifa, tarjeta y recaudo',                  'ALTA',    TRUE),
  ('INFRAESTRUCTURA',     'Estaciones, paraderos e infraestructura',    'BAJA',    TRUE),
  ('ACCESIBILIDAD',       'Accesibilidad para población con movilidad reducida', 'ALTA', TRUE),
  ('OTROS',               'Otros asuntos',                              'BAJA',    TRUE);

INSERT INTO estado_pqrs (codigo, nombre, es_final, orden) VALUES
  ('RADICADA',   'Radicada',              FALSE, 1),
  ('EN_TRAMITE', 'En trámite',            FALSE, 2),
  ('RESUELTA',   'Resuelta',              FALSE, 3),
  ('CERRADA',    'Cerrada',               TRUE,  4),
  ('VENCIDA',    'Vencida sin respuesta', FALSE, 5);

INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_nominal) VALUES
  ('ARTICULADO',  'Bus articulado troncal',    160),
  ('PADRON',      'Bus padrón',                 80),
  ('ALIMENTADOR', 'Bus alimentador',            50);

INSERT INTO tipo_incidencia (codigo, nombre, prioridad_base) VALUES
  ('FALLA_MECANICA',    'Falla mecánica en servicio',     'ALTA'),
  ('RETRASO',           'Retraso superior al programado', 'MEDIA'),
  ('ACCIDENTE_VIAL',    'Accidente vial',                 'CRITICA'),
  ('VANDALISMO',        'Vandalismo sobre el vehículo',   'ALTA'),
  ('AGLOMERACION',      'Aglomeración por sobredemanda',  'MEDIA'),
  ('DESVIO_RUTA',       'Desvío no autorizado de ruta',   'ALTA');

INSERT INTO rol (codigo, nombre, descripcion) VALUES
  ('ROLE_ADMIN',        'Administrador',       'Administra catálogos, flota y usuarios del sistema'),
  ('ROLE_FUNCIONARIO',  'Funcionario de PQRS', 'Gestiona, responde y cierra las solicitudes ciudadanas'),
  ('ROLE_AUDITOR',      'Auditor',             'Consulta indicadores y trazabilidad en modo solo lectura');

-- Consecutivo del radicado para el año en curso.
INSERT INTO consecutivo_radicado (anio, ultimo_valor) VALUES (2026, 0), (2027, 0);

-- ---------------------------------------------------------------------
-- Calendario de festivos de Colombia (Ley 51 de 1983 — Ley Emiliani).
-- Los festivos trasladables se registran ya corridos al lunes siguiente.
-- ---------------------------------------------------------------------

INSERT INTO festivo (fecha, descripcion) VALUES
  (DATE '2026-01-01', 'Año Nuevo'),
  (DATE '2026-01-12', 'Día de los Reyes Magos'),
  (DATE '2026-03-23', 'Día de San José'),
  (DATE '2026-04-02', 'Jueves Santo'),
  (DATE '2026-04-03', 'Viernes Santo'),
  (DATE '2026-05-01', 'Día del Trabajo'),
  (DATE '2026-05-18', 'Ascensión del Señor'),
  (DATE '2026-06-08', 'Corpus Christi'),
  (DATE '2026-06-15', 'Sagrado Corazón de Jesús'),
  (DATE '2026-06-29', 'San Pedro y San Pablo'),
  (DATE '2026-07-20', 'Declaratoria de la Independencia'),
  (DATE '2026-08-07', 'Batalla de Boyacá'),
  (DATE '2026-08-17', 'Asunción de la Virgen'),
  (DATE '2026-10-12', 'Día de la Raza'),
  (DATE '2026-11-02', 'Todos los Santos'),
  (DATE '2026-11-16', 'Independencia de Cartagena'),
  (DATE '2026-12-08', 'Inmaculada Concepción'),
  (DATE '2026-12-25', 'Navidad'),
  (DATE '2027-01-01', 'Año Nuevo'),
  (DATE '2027-01-11', 'Día de los Reyes Magos'),
  (DATE '2027-03-22', 'Día de San José'),
  (DATE '2027-03-25', 'Jueves Santo'),
  (DATE '2027-03-26', 'Viernes Santo'),
  (DATE '2027-05-01', 'Día del Trabajo'),
  (DATE '2027-05-10', 'Ascensión del Señor'),
  (DATE '2027-05-31', 'Corpus Christi'),
  (DATE '2027-06-07', 'Sagrado Corazón de Jesús'),
  (DATE '2027-07-05', 'San Pedro y San Pablo'),
  (DATE '2027-07-20', 'Declaratoria de la Independencia'),
  (DATE '2027-08-07', 'Batalla de Boyacá'),
  (DATE '2027-08-16', 'Asunción de la Virgen'),
  (DATE '2027-10-18', 'Día de la Raza'),
  (DATE '2027-11-01', 'Todos los Santos'),
  (DATE '2027-11-15', 'Independencia de Cartagena'),
  (DATE '2027-12-08', 'Inmaculada Concepción'),
  (DATE '2027-12-25', 'Navidad');
