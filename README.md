# Servicio de PQRS e incidencias de flota — Transcaribe S.A.

API REST para la radicación, el trámite y el seguimiento de peticiones, quejas, reclamos y
sugerencias del Sistema Integrado de Transporte Masivo de Cartagena, con registro de
incidencias operativas de la flota.

> **El problema que resuelve.** Transcaribe reportó 5.370 PQRS atendidas en la vigencia 2024
> y reconoció haber encontrado más de mil solicitudes de años anteriores represadas y sin
> responder. Con hojas de cálculo, el plazo legal de cada solicitud se calcula a mano, nadie
> puede decir cuántas vencen esta semana y la traza de lo ocurrido queda repartida entre
> varias bandejas de correo.

### Servicio publicado

| | |
|---|---|
| **API** | https://transcaribe-pqrs-api.onrender.com |
| **Documentación interactiva** | [https://transcaribe-pqrs-api.onrender.com/swagger-ui.html](https://transcaribe-pqrs-api.onrender.com/swagger-ui.html) |
| **Salud** | [https://transcaribe-pqrs-api.onrender.com/actuator/health](https://transcaribe-pqrs-api.onrender.com/actuator/health) |
| **Catálogo con los plazos legales** | [https://transcaribe-pqrs-api.onrender.com/api/v1/catalogos/tipos-solicitud](https://transcaribe-pqrs-api.onrender.com/api/v1/catalogos/tipos-solicitud) |

La raíz `/` responde `401`: no es una ruta pública. Todo lo que no está explícitamente
abierto exige autenticación, y eso incluye la portada. En el plan gratuito el servicio se
duerme tras quince minutos sin tráfico, así que la primera petición puede tardar.

**Trabajo Colaborativo Contextualizado** · Ingeniería de Software · Universidad de Cartagena

- Rosary Eilyn Carmona Navarro — `rcarmonan@unicartagena.edu.co`
- Moisés Daniel Ortega Sánchez — `mortegas4@unicartagena.edu.co`
- José Luis Arnedo Zambrano — `jarnedoz@unicartagena.edu.co`

---

## Qué hace

- **Calcula el plazo legal solo.** Días hábiles según el artículo 14 de la Ley 1755 de 2015,
  descontando fines de semana y los festivos de Colombia ya trasladados por la Ley 51 de 1983.
- **Asigna el radicado.** Único, consecutivo dentro del año, bajo bloqueo de fila para que dos
  radicaciones simultáneas no reciban el mismo número.
- **Impide las transiciones ilegales.** Una solicitud cerrada no admite cambios; de radicada no
  se salta a cerrada; ningún estado transita hacia sí mismo.
- **Deja traza de todo.** Cada cambio de estado queda registrado con su autor, su observación y
  su instante, en una bitácora que no tiene métodos de modificación.
- **Cruza la queja con el hecho.** Una incidencia de flota puede vincularse con la PQRS del
  ciudadano que la presenció.
- **Mide el cumplimiento.** Porcentaje de solicitudes respondidas dentro del término, calculado
  sobre un dato registrado al responder, no inferido después.

---

## Arranque rápido

Hay tres formas de levantarlo y las tres aplican **las mismas migraciones de Flyway**. No existe
un esquema paralelo para desarrollo: si una migración está mal, falla en todas.

| Dónde | Qué hace falta | Base de datos |
|---|---|---|
| En la máquina, sin Docker | JDK 21 y nada más | H2 embebida, en un archivo local |
| En la máquina, con Docker | Docker Desktop | PostgreSQL 16 en contenedor |
| En Render | una cuenta de Render | PostgreSQL gestionado, en un esquema propio |

### 1. Sin Docker ni PostgreSQL instalado

```bash
./mvnw -Plocal spring-boot:run
```

Eso es todo. El perfil `local` levanta el servicio completo sobre una base H2 en modo de
compatibilidad PostgreSQL, con el esquema real y los datos de demostración. Los datos quedan en
`datos/pqrs.mv.db` y sobreviven a los reinicios; para empezar de cero, bórrese esa carpeta.

Existe por una razón práctica: una demostración no puede depender de que el demonio de Docker
arranque en una máquina ajena. H2 **no** sustituye a PostgreSQL para medir nada —la verificación
que cuenta corre contra PostgreSQL real—, pero sirve para enseñar el sistema funcionando.

El motor H2 no entra en la imagen de producción: el perfil de Maven `local` se limita a arrancar
con el classpath de pruebas, donde ya estaba.

### 2. Con Docker

```bash
cp .env.ejemplo .env     # y cambie los tres valores marcados CAMBIAR
docker compose up --build
```

El servicio queda en `http://localhost:8080` con PostgreSQL 16 real detrás. La base **no** se
publica al exterior: su puerto se enlaza solo a `127.0.0.1`.

**Variante, contra un PostgreSQL que ya tenga instalado:**

```bash
export DB_URL=jdbc:postgresql://localhost:5432/transcaribe_pqrs
export DB_USER=transcaribe
export DB_PASSWORD=<su-contraseña>

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Los perfiles `local` y `dev` cargan los datos de demostración (rutas, vehículos y tres usuarios).
**Nunca se cargan en producción**: viven en una ruta de migración aparte.

### 3. En Render

El repositorio incluye [`render.yaml`](render.yaml): en Render, **New → Blueprint**, se conecta
este repositorio y se confirma. El plano declara el servicio web y lo construye con el **mismo
Dockerfile** que se usa en local, de modo que lo verificado en la máquina es lo que se despliega.
La clave de firma se genera sola; ninguna credencial pasa por el repositorio.

El plano **no** crea la base: Render solo admite una gratuita por cuenta, así que el servicio
reutiliza la que exista y se instala en un esquema propio dentro de ella (`DB_ESQUEMA`). Pide
cuatro valores al aplicarlo —`DB_HOST`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`— y, si falta alguno,
el arranque se detiene diciendo cuál, en vez de intentar conectarse a `localhost`.

Detalle completo, incluidas las limitaciones del plan gratuito, en
[docs/DESPLIEGUE_RENDER.md](docs/DESPLIEGUE_RENDER.md).

### Comprobar que arrancó

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/catalogos/tipos-solicitud
```

Documentación interactiva del contrato: `http://localhost:8080/swagger-ui.html`
(desactivada en el perfil `prod`, a propósito).

---

## Primer recorrido en tres pasos

**1. Radicar una queja** — sin autenticarse, porque el derecho de petición no puede exigir cuenta:

```bash
curl -X POST http://localhost:8080/api/v1/pqrs \
  -H "Content-Type: application/json" \
  -d '{
    "ciudadano": {
      "tipoDocumento": "CC", "numeroDocumento": "1047398221",
      "nombres": "Carlos Andrés", "apellidos": "Pacheco",
      "correo": "capacheco@correo.com", "telefono": "3015558841"
    },
    "tipoSolicitud": "QUEJA", "categoria": "SERVICIO_RUTA", "canal": "WEB",
    "asunto": "La ruta T100 no cumple la frecuencia anunciada",
    "descripcion": "El bus pasó con cuarenta minutos de retraso en hora pico.",
    "codigoRuta": "T100", "placaVehiculo": "UXT101"
  }'
```

La respuesta trae el radicado, la prioridad y la fecha de vencimiento, los tres calculados por
el servidor. Si los envía usted en la petición, se rechaza: el contrato no admite campos no
declarados.

**2. Consultar como ciudadano** — hacen falta el radicado **y** el documento del titular:

```bash
curl "http://localhost:8080/api/v1/pqrs/publico/TC-2026-000001?documento=1047398221"
```

Con el documento de otra persona responde `404`, el mismo error que para un radicado
inexistente. Así, probar radicados al azar no revela cuáles existen.

**3. Tramitar como funcionario**:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/autenticacion/iniciar-sesion \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"jlopez","contrasena":"Transc@ribe2026.Fun"}' | jq -r .token)

curl -X PATCH http://localhost:8080/api/v1/pqrs/TC-2026-000001/estado \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"estadoDestino":"EN_TRAMITE","observacion":"Asignada al área de operaciones"}'
```

---

## Verificación

```bash
./mvnw verify                      # 162 pruebas + informe de cobertura
```

Qué verifica cada nivel, con qué datos y cómo reproducirlo operación por operación: [docs/GUIA_PRUEBAS.md](docs/GUIA_PRUEBAS.md). **Las 22 operaciones del contrato están cubiertas** por la colección de Postman.

La colección de Postman en [`postman/`](postman/) es ejecutable de principio a fin con el
Runner: 34 peticiones y 95 aserciones. Guarda el testigo y el radicado en variables de
colección, de modo que no hay que editar nada entre peticiones. No comprueba solo los caminos
felices: incluye peticiones diseñadas para fallar y verifica que fallan por el motivo correcto.

Análisis de dependencias vulnerables (necesita clave de la NVD):

```bash
./mvnw verify -Pseguridad -Dnvd.api.key=SU_CLAVE
```

---

## Cómo está organizado

```
src/main/java/co/edu/unicartagena/transcaribe/pqrs/
├── domain/              Reglas de negocio. Sin una sola anotación de framework.
│   ├── model/           Agregados, entidades y enumeraciones
│   ├── port/            Interfaces que el dominio DECLARA y la infraestructura implementa
│   ├── service/         Calendario hábil, política de prioridad, generador de radicado
│   └── exception/       Jerarquía de errores de negocio
├── application/         Casos de uso. Orquesta; no decide.
│   ├── usecase/         Puertos de entrada (RadicarPqrs, GestionarPqrs, ConsultarPqrs)
│   ├── service/         Implementaciones
│   ├── dto/             Contratos de entrada y salida, con validación estricta
│   └── mapper/          Traducción a las vistas del API
└── infrastructure/      Los bordes del sistema
    ├── web/             Controladores y manejador global de errores
    ├── security/        Cadena de filtros, JWT, límite de peticiones
    ├── persistence/     Adaptadores JPA que satisfacen los puertos
    └── config/          Propiedades, tareas programadas, OpenAPI
```

La dependencia apunta **siempre hacia el dominio**. La capa de persistencia depende del
dominio, no al revés: por eso las 154 pruebas que no son de integración se ejecutan en unos
doce segundos, sin levantar Spring ni base de datos.

Detalle completo en el [manual de usuario y operación](docs/MANUAL_USUARIO.md) y en los
[diagramas](docs/diagramas/), que se **generan con código** (`docs/diagramas/generar_diagramas.py`)
para que puedan rehacerse cuando el diseño cambie.

---

## Seguridad

Controles implementados frente al OWASP Top 10:2021. El detalle de cada uno, con su
justificación, está en el documento del proyecto y en los comentarios del código.

| Riesgo | Control |
|---|---|
| A01 Control de acceso | Consulta pública con radicado **y** documento, comparación en tiempo constante, datos enmascarados, autorización por método |
| A02 Criptografía | BCrypt coste 12; IP almacenada como resumen SHA-256 con sal; HSTS |
| A03 Inyección | Parámetros vinculados y API de criterios; nunca concatenación. Validación por lista de permitidos |
| A04 Diseño inseguro | Límite de peticiones por minuto, tamaño de página acotado, máquina de estados |
| A05 Configuración | Usuarios de demostración fuera de la ruta de producción; errores sin traza ni detalles del motor |
| A06 Componentes | OWASP Dependency-Check en perfil propio, falla con CVSS ≥ 7 |
| A07 Autenticación | Error indistinguible, verificación señuelo contra ataques por tiempo, bloqueo tras 5 intentos |
| A08 Integridad | Rechazo de campos no declarados; radicado, prioridad y vencimiento los fija el servidor |
| A09 Registro | Bitácora inmutable; auditoría en transacción propia, sobrevive al error que la provocó |
| A10 SSRF | No aplica: el servicio no invoca direcciones suministradas por el usuario |

**Lo que este sistema no resuelve**, dicho explícitamente: el limitador de peticiones guarda
sus contadores en memoria del proceso, así que con varias réplicas cada una contaría por
separado; y la tarea que marca solicitudes vencidas se ejecutaría en todas las instancias.
Las transiciones son idempotentes y no se corrompe nada, pero el trabajo se duplicaría. Con
más de una réplica harían falta un contador compartido y un bloqueo distribuido.

---

## Tecnologías

Java 21 · Spring Boot 3.3.5 · Spring Security · JPA/Hibernate · PostgreSQL 16 · Flyway ·
JWT · OpenAPI · JUnit 5 · Mockito · AssertJ · H2 (pruebas) · Docker · GitHub Actions

---

## Licencia

MIT. Trabajo académico de la Universidad de Cartagena.

Transcaribe S.A. es una entidad real y las cifras citadas provienen de fuentes públicas, pero
**este proyecto no fue encargado por la entidad ni está implantado en su operación**. Los
datos de rutas, vehículos y ciudadanos usados en las pruebas son representativos y ficticios.
