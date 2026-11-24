# Manual de usuario y operación

Servicio de PQRS e incidencias de flota — Transcaribe S.A.
Versión 1.0.0 · Diciembre de 2026

Este manual cubre dos cosas que suelen separarse y conviene leer juntas: **cómo se opera** el
sistema y **cómo está construido**. Quien atiende solicitudes necesita lo primero; quien debe
mantenerlo o auditarlo necesita lo segundo.

---

## Índice

1. [A quién sirve y qué puede hacer cada quien](#1-a-quién-sirve-y-qué-puede-hacer-cada-quien)
2. [Operación diaria](#2-operación-diaria)
3. [Reglas que el sistema no deja incumplir](#3-reglas-que-el-sistema-no-deja-incumplir)
4. [Arquitectura del software](#4-arquitectura-del-software)
5. [Catálogo de operaciones del API](#5-catálogo-de-operaciones-del-api)
6. [Puesta en marcha y configuración](#6-puesta-en-marcha-y-configuración)
7. [Qué hacer cuando algo falla](#7-qué-hacer-cuando-algo-falla)

---

## 1. A quién sirve y qué puede hacer cada quien

El sistema distingue cinco actores. Los tres primeros son personas; los dos últimos no.

| Actor | Se autentica | Qué puede hacer |
|---|---|---|
| **Ciudadano** | No | Radicar una solicitud y consultar la suya aportando radicado y documento |
| **Funcionario de PQRS** | Sí, rol `ROLE_FUNCIONARIO` | Tramitar, responder, listar, registrar incidencias |
| **Administrador** | Sí, rol `ROLE_ADMIN` | Todo lo anterior, más administrar flota, rutas, asignaciones e indicadores |
| **Auditor** | Sí, rol `ROLE_AUDITOR` | Solo lectura: consultas, trazabilidad e indicadores |
| **Programador de tareas** | — | Marca cada mañana las solicitudes vencidas y refresca el calendario de festivos |

**Por qué el ciudadano no tiene cuenta.** El derecho de petición es un derecho fundamental.
Condicionarlo a registrarse en un sistema sería ponerle una barrera que la ley no contempla.
Por eso radicar y consultar son operaciones anónimas, y la protección del dato personal se
resuelve de otra forma: exigiendo el documento del titular junto al radicado.

---

## 2. Operación diaria

### 2.1. El ciudadano radica

El ciudadano entrega sus datos de identificación, elige el tipo de solicitud y la categoría,
y escribe su asunto y descripción. Si su queja se refiere a un bus o una ruta concretos, puede
indicarlos.

El sistema le devuelve de inmediato:

- el **número de radicado**, con formato `TC-AAAA-NNNNNN`;
- la **fecha de vencimiento** del término legal, ya calculada;
- el **fundamento legal** del plazo que se le aplicó.

Ese último dato importa más de lo que parece: el ciudadano tiene derecho a saber en cuántos
días hábiles debe recibir respuesta y por qué.

### 2.2. El funcionario tramita

Al entrar, el funcionario obtiene un testigo de acceso válido por treinta minutos. Con él
puede listar solicitudes filtrando por estado, prioridad, ruta, placa, documento o rango de
fechas, y ordenar por fecha de radicación, de vencimiento o por prioridad.

El flujo normal de una solicitud es:

```
RADICADA  ──►  EN_TRAMITE  ──►  RESUELTA  ──►  CERRADA
    │              │              │
    └──► VENCIDA ◄─┘              │
            │                     │
            └──► EN_TRAMITE ──────┘
```

- **Pasar a trámite** exige una observación de al menos cinco caracteres.
- **Responder** registra la respuesta de fondo y deja la solicitud resuelta en un solo paso.
- **Cerrar** exige una justificación de al menos veinte caracteres. Es el único paso
  irreversible: una solicitud cerrada no admite ningún cambio posterior.

Cada una de esas acciones queda en la bitácora con el nombre del funcionario, la observación
y el instante exacto. La bitácora se consulta en `/api/v1/pqrs/{radicado}/seguimientos`.

### 2.3. Registro de incidencias

Cuando ocurre un hecho operativo —una falla mecánica, un retraso, un accidente— se registra
como incidencia contra la placa del vehículo. Si existe una PQRS ciudadana sobre el mismo
hecho, se vinculan.

Ese vínculo es el que permite responder una pregunta que de otro modo queda sin respuesta:
¿las quejas por frecuencia de esta ruta corresponden a fallas reales de vehículos concretos o
a percepciones aisladas?

### 2.4. Indicadores

En `/api/v1/indicadores`, los roles de administración y auditoría obtienen:

- total de solicitudes radicadas en el periodo;
- conteo por estado;
- resueltas dentro y fuera del término;
- **porcentaje de cumplimiento de oportunidad**;
- vencidas sin respuesta a la fecha;
- incidencias del año.

El porcentaje de oportunidad se calcula sobre una marca que se fija **en el momento de
responder**, no comparando fechas después. La razón es jurídica: el cumplimiento debe juzgarse
con el calendario vigente el día en que se respondió.

---

## 3. Reglas que el sistema no deja incumplir

Estas doce reglas están implementadas, no documentadas como intención. Cada una es rastreable
hasta la clase que la custodia.

| ID | Regla | Dónde vive |
|---|---|---|
| RN-01 | 15 días hábiles para peticiones, quejas, reclamos, sugerencias y denuncias; 10 para documentos; 30 para consultas | `TipoSolicitud` + `CalendarioHabilColombiano` |
| RN-02 | Radicado único y consecutivo por año | `GeneradorRadicadoConsecutivo` |
| RN-03 | Una solicitud cerrada no admite cambios | `Pqrs.verificarQueNoEsteCerrada` |
| RN-04 | Solo las transiciones del grafo; ninguna hacia sí misma | `CodigoEstadoPqrs` |
| RN-05 | La placa señalada debe pertenecer a la flota | `RadicacionPqrsService.resolverVehiculo` |
| RN-06 | La prioridad la fija el sistema; una denuncia nunca baja de ALTA | `PoliticaPrioridad` |
| RN-07 | Un ciudadano es único por tipo y número de documento | `RadicacionPqrsService.resolverCiudadano` |
| RN-08 | El cálculo excluye sábados, domingos y festivos trasladados | `CalendarioHabilColombiano` |
| RN-09 | Toda transición queda en la bitácora con autor e instante | `Pqrs.cambiarEstado` |
| RN-10 | El cierre exige justificación de 20 caracteres | `Pqrs.exigirJustificacion` |
| RN-11 | La consulta ciudadana exige radicado y documento, y enmascara | `ConsultaPqrsService` |
| RN-12 | Una incidencia no puede fecharse en el futuro | `GestionIncidenciasService` |

### Sobre el cálculo de días hábiles

El artículo 14 de la Ley 1755 de 2015 fija los términos pero no dice literalmente que los días
sean hábiles. La interpretación que aplica este sistema —y que es la mayoritaria en la práctica
administrativa— se apoya en el artículo 62 de la Ley 4 de 1913: cuando una norma fija un plazo
en días sin precisar, se entienden días hábiles.

Los festivos no se calculan en el código: viven en la tabla `festivo`, ya trasladados al lunes
según la Ley 51 de 1983. Así, corregir el calendario de un año es modificar filas, no
redesplegar el servicio. El sistema recarga el calendario cada madrugada y, si la recarga
falla, **conserva el anterior**: quedarse sin festivos produciría vencimientos más cortos de
lo que permite la ley.

---

## 4. Arquitectura del software

### 4.1. Las cuatro capas

```
  ┌─────────────────────────────────────────────┐
  │  infrastructure.web      controladores      │
  ├─────────────────────────────────────────────┤
  │  infrastructure.security cadena de filtros  │
  ├─────────────────────────────────────────────┤
  │  application             casos de uso       │
  ├─────────────────────────────────────────────┤
  │  domain                  REGLAS             │ ◄── nada depende de fuera
  ├─────────────────────────────────────────────┤
  │  infrastructure.persistence   adaptadores   │ ──► implementa los puertos
  └─────────────────────────────────────────────┘
```

La capa de persistencia aparece **debajo** del dominio y la flecha apunta hacia arriba. No es
un error del dibujo: el dominio declara interfaces (`PqrsRepositoryPort`,
`CatalogoRepositoryPort`, `AuditoriaPort`) y la infraestructura las implementa. La dependencia
va del detalle hacia la regla, nunca al revés.

**Consecuencia comprobable:** 160 de las 168 pruebas se ejecutan en unos 12 segundos
porque no necesitan Spring ni base de datos. Si el dominio tuviera anotaciones del framework,
cada prueba exigiría levantar un contexto.

### 4.2. Decisiones que conviene conocer antes de tocar el código

**El dominio no conoce Spring.** Las clases de `domain` se instancian en
`ConfiguracionDominio`, una clase de configuración situada en el borde. Si añade una regla,
no la anote: regístrela ahí.

**El tiempo se inyecta.** Ninguna regla llama a `LocalDateTime.now()`. Todas reciben el
instante mediante la interfaz `Reloj`. Una regla que consulta el reloj del sistema no se puede
probar. La única excepción deliberada es la emisión de testigos JWT, cuya vigencia la verifica
el receptor contra su propio reloj.

**El agregado custodia su estado.** `Pqrs` no tiene setters públicos. Toda modificación pasa
por un método que valida la regla y deja la traza. Es la única forma de garantizar que el
estado y la bitácora nunca se desincronicen.

**Los catálogos son inmutables en ejecución.** Las tablas de tipos, estados y categorías se
administran por migración de Flyway, no por el API. Así, un fallo de autorización no puede
alterar los plazos legales del sistema.

**La paginación es propia.** El dominio define `Pagina` y `Paginacion` en vez de usar los
tipos de Spring Data. Cuesta cuarenta líneas y a cambio el contrato del API no cambia porque
se actualice una dependencia.

### 4.3. Modelo de datos

Diecinueve tablas normalizadas hasta tercera forma normal. Los dos puntos donde la
normalización hace trabajo real:

- El **plazo legal** vive en `tipo_solicitud`, no en cada PQRS. Una reforma normativa se
  aplica cambiando una fila.
- La **capacidad nominal** vive en `tipo_vehiculo`, no en cada bus.

Y un punto donde deliberadamente **no** se normaliza: la columna `respondida_en_plazo` de
`pqrs` almacena un hecho registrado al responder. No es un dato derivado redundante; es la
foto del cumplimiento en el momento en que ocurrió.

El diagrama completo está en [`diagramas/02-entidad-relacion.png`](diagramas/02-entidad-relacion.png).

---

## 5. Catálogo de operaciones del API

Base: `/api/v1`. Los contratos completos, con esquemas y ejemplos, están en
`/swagger-ui.html` cuando el servicio corre en perfil `dev`.

### Público

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/pqrs` | Radica una solicitud |
| `GET` | `/pqrs/publico/{radicado}?documento=` | Consulta ciudadana, con datos enmascarados |
| `GET` | `/catalogos/tipos-solicitud` | Tipos con su plazo legal y fundamento |
| `GET` | `/catalogos/categorias` | Categorías temáticas activas |
| `GET` | `/catalogos/canales` | Canales de recepción |
| `GET` | `/rutas` | Rutas activas del sistema |
| `POST` | `/autenticacion/iniciar-sesion` | Obtiene el testigo de acceso |

### Funcionario, administrador o auditor

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/pqrs/{radicado}` | Detalle completo |
| `GET` | `/pqrs` | Listado paginado con filtros |
| `GET` | `/pqrs/{radicado}/seguimientos` | Bitácora de transiciones |
| `GET` | `/vehiculos`, `/vehiculos/{placa}` | Consulta de flota |
| `GET` | `/incidencias` | Listado de incidencias |

### Funcionario o administrador

| Método | Ruta | Qué hace |
|---|---|---|
| `PATCH` | `/pqrs/{radicado}/estado` | Cambia el estado |
| `POST` | `/pqrs/{radicado}/respuesta` | Registra la respuesta de fondo |
| `POST` | `/incidencias` | Registra una incidencia |
| `PATCH` | `/incidencias/{consecutivo}/atencion` | La toma en atención |
| `PATCH` | `/incidencias/{consecutivo}/cierre` | La cierra |

### Solo administrador

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/pqrs/{radicado}/asignacion?funcionario=` | Asigna la solicitud |
| `POST` | `/vehiculos` | Vincula un vehículo |
| `POST` | `/rutas` | Registra una ruta |

### Administrador o auditor

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/indicadores?desde=&hasta=` | Indicadores de gestión |

### Códigos de respuesta

| Código | Significado en este servicio |
|---|---|
| `201` | Recurso creado; la cabecera `Location` apunta a él |
| `400` | La petición no cumple el contrato. El cuerpo detalla campo a campo |
| `401` | Falta el testigo o no es válido |
| `403` | El testigo es válido pero el rol no alcanza |
| `404` | No existe, o quien consulta no tiene por qué verlo |
| `409` | Conflicto con información ya registrada |
| `422` | La petición es correcta pero una regla de negocio lo impide |
| `429` | Se excedió el límite de peticiones por minuto |

Todo error trae un **código de rastreo** de ocho caracteres. Cítelo al reportar un problema:
con él, el operador encuentra la traza completa del servidor sin que el mensaje haya revelado
nada al cliente.

---

## 6. Puesta en marcha y configuración

### Dónde está desplegado

El servicio está publicado en **https://transcaribe-pqrs-api.onrender.com**, construido desde el mismo Dockerfile que se usa en
local. La documentación interactiva del contrato queda en `/swagger-ui.html`.

La raíz `/` responde `401` a propósito: no figura entre las rutas públicas. Las que sí lo son
se listan en el apartado 5.

### Variables de entorno

| Variable | Obligatoria | Para qué sirve |
|---|---|---|
| `DATABASE_URL` | Una de las dos vías | Conexión entera en el formato del proveedor: `postgresql://usuario:clave@servidor/base`. El servicio la traduce |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | La otra vía | Las mismas piezas por separado. Lo definido aquí manda sobre lo derivado de `DATABASE_URL` |
| `DB_URL` | Alternativa | Cadena JDBC completa, para un proveedor que ya la entregue así |
| `DB_ESQUEMA` | Recomendada | Espacio de nombres propio dentro de la base. Permite compartir instancia sin mezclar tablas |
| `JWT_SECRET` | **Sí en producción** | Clave de firma. **Mínimo 32 bytes**: el servicio no arranca si es más corta |
| `CORS_ORIGENES` | Recomendada | Orígenes permitidos, separados por coma. No se admite `*` |
| `RATE_LIMIT_POR_MINUTO` | No | Por omisión 60 |
| `PORT` | No | Por omisión 8080. En la nube la inyecta el proveedor |
| `ADMIN_USUARIO`, `ADMIN_CORREO`, `ADMIN_CONTRASENA` | En un despliegue nuevo | Crean la primera cuenta de administración si la tabla de usuarios está vacía. La contraseña necesita 12 caracteres o más |
| `SWAGGER_ENABLED` | No | Desactivado por omisión en `prod`; el despliegue publicado lo activa a propósito, para que el contrato pueda explorarse |

Si faltan las credenciales de la base, el servicio **no arranca**: se detiene nombrando las
variables que faltan, en lugar de intentar conectarse a `localhost` y fallar con un error de
red que no dice nada.

Genere la clave de firma con:

```bash
openssl rand -base64 48
```

### Perfiles

| Perfil | Base de datos | Qué carga |
|---|---|---|
| `local` | H2 en un archivo (`datos/pqrs.mv.db`) | Esquema + catálogos + **datos de demostración**. Sin Docker ni PostgreSQL instalado |
| `dev` | PostgreSQL en `localhost:5432` | Lo mismo, contra un motor real. Swagger activo, trazas en DEBUG |
| `test` | H2 en memoria | La suite automatizada. Límite de peticiones desactivado |
| `prod` | PostgreSQL por variables de entorno | Solo esquema y catálogos. Swagger desactivado, trazas en WARN |

Los cuatro aplican **las mismas migraciones**: no hay un esquema distinto para desarrollar.

El perfil `local` se activa con `./mvnw -Plocal spring-boot:run` y existe para que una
demostración no dependa de que el demonio de Docker arranque en una máquina ajena. H2 corre en
modo de compatibilidad PostgreSQL, que cubre lo que este esquema usa, pero **no sustituye al
motor real para medir nada**: la verificación que cuenta sigue siendo la que corre contra
PostgreSQL, en Docker o en la nube. Para empezar de cero, bórrese la carpeta `datos/`.

### Credenciales de demostración

Solo existen en `local`, `dev` y `test`:

Las tres cuentas son las de los integrantes del equipo, con el rol que corresponde a la capa que
trabajó cada uno. El nombre de usuario es el de su correo institucional.

| Usuario | Persona | Contraseña | Rol |
|---|---|---|---|
| `rcarmonan` | Rosary Eilyn Carmona Navarro | `Transc@ribe2026.Adm` | ADMIN + FUNCIONARIO |
| `jarnedoz` | José Luis Arnedo Zambrano | `Transc@ribe2026.Fun` | FUNCIONARIO |
| `mortegas4` | Moisés Daniel Ortega Sánchez | `Transc@ribe2026.Aud` | AUDITOR |

Conviene tener presente el reparto al probar: los indicadores exigen ADMIN **o** AUDITOR, así que
`jarnedoz` recibe 403 aunque se haya autenticado correctamente. No es un fallo, es la regla
funcionando: 401 significa «identifíquese» y 403, «está identificado y aun así no le
corresponde».

Están en `src/main/resources/db/demo/`, una ruta que el perfil `prod` **no incluye**. Es una
decisión de seguridad: mantener las cuentas conocidas fuera del camino de migración de
producción cierra el riesgo de desplegarlas sin querer.

### Guía de pruebas

Los datos de demostración, la matriz de cobertura de las veintidós operaciones y las pruebas que fallan a propósito están en [GUIA_PRUEBAS.md](GUIA_PRUEBAS.md).

### Migraciones

El esquema lo gobierna Flyway; Hibernate solo lo valida (`ddl-auto: validate`). Si una entidad
y la tabla dejan de coincidir, el servicio **no arranca**, que es preferible a que arranque y
falle en la primera consulta.

Para añadir un cambio de esquema, cree `V3__descripcion.sql`. Nunca modifique una migración
ya aplicada.

---

## 7. Qué hacer cuando algo falla

**El servicio no arranca y la traza menciona la clave de firma.**
`JWT_SECRET` falta o tiene menos de 32 bytes. Es deliberado: un servicio que arranca con una
clave vacía parece sano y no lo está.

**El servicio no arranca y Hibernate se queja de una columna.**
El esquema de la base y las entidades no coinciden. Revise si falta aplicar una migración o si
alguien modificó la tabla a mano.

**Las fechas de vencimiento salen más cortas de lo esperado.**
Compruebe que la tabla `festivo` tiene cargado el año en curso. El servicio registra en la
traza cuántos festivos cargó al arrancar: `Calendario de días hábiles cargado: N festivos`.

**Un funcionario no puede entrar aunque la contraseña es correcta.**
Probablemente la cuenta está bloqueada por intentos fallidos. El bloqueo dura quince minutos y
se levanta solo. Para confirmarlo, consulte `bloqueado_hasta` en la tabla `usuario`.

**Un cliente recibe `429`.**
Excedió el límite de peticiones por minuto. Si es legítimo, suba `RATE_LIMIT_POR_MINUTO`.

**Las respuestas de error no dicen qué pasó.**
Es intencional. El detalle está del lado del servidor, asociado al código de rastreo que el
cliente recibió. Búsquelo en las trazas: `[a1b2c3d4]`.
