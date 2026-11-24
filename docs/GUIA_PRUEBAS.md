# Guía de pruebas

Qué se verifica, con qué datos, y dónde está escrita cada comprobación. El propósito de este
documento es que cualquiera pueda **reproducir** la verificación, no que se fíe de ella.

Dos niveles de evidencia, y conviene no confundirlos. Las pruebas automatizadas con JUnit
comprueban el comportamiento del código sin red de por medio. La colección de Postman comprueba
el servicio **ya desplegado**, por HTTP, con su seguridad activa. Una no sustituye a la otra: la
primera encuentra reglas mal escritas, la segunda encuentra servicios mal configurados.

---

## 1. Los cuatro niveles automatizados

```bash
./mvnw verify
```

Son **168 pruebas** y tardan alrededor de un minuto. El reparto no es decorativo: cada nivel
responde a una pregunta distinta.

| Nivel | Pruebas | Qué responde | Levanta contexto |
|---|---|---|---|
| Dominio | 105 | ¿La regla de negocio es correcta? | No |
| Aplicación | 30 | ¿El caso de uso orquesta bien, con los puertos sustituidos por dobles? | No |
| Infraestructura | 25 | ¿La configuración del despliegue falla pronto y con un mensaje útil? | No |
| Integración | 8 | ¿El API completo funciona sobre una base real con el esquema aplicado? | Sí |

Las 160 que no levantan contexto tardan unos doce segundos; las ocho de integración, casi un
minuto. Esa diferencia es la razón práctica de mantener el dominio libre de anotaciones del
marco de trabajo.

Para ejecutar un solo nivel:

```bash
./mvnw test -Dtest='*Test'                    # todo
./mvnw test -Dtest='CalendarioHabilColombianoTest'   # una clase
./mvnw test -Dtest='PqrsApiIntegracionTest'          # solo integración
```

El informe de cobertura queda en `target/site/jacoco/index.html` tras `./mvnw verify`.

---

## 2. La colección de Postman

Ejercita el servicio desplegado. **34 peticiones y 95 aserciones**, organizadas en ocho carpetas
que se ejecutan en orden: cada una deja preparado lo que la siguiente necesita.

```bash
docker compose up --build -d
newman run postman/Transcaribe-PQRS.postman_collection.json
```

También se puede abrir en Postman y usar el *Runner* sobre la colección completa.

**Es ejecutable las veces que haga falta sobre la misma base de datos.** Las altas de ruta y de
vehículo generan identificadores únicos a partir del reloj, porque un alta repetida con el mismo
código responde `409` y haría fallar la segunda ejecución. Eso se comprobó corriéndola tres
veces seguidas sin reiniciar la base.

Variables de la colección:

| Variable | Para qué |
|---|---|
| `baseUrl` | A dónde apunta. Por omisión `http://localhost:8080` |
| `token` | El testigo JWT. Lo fijan las peticiones de inicio de sesión |
| `radicado` | El radicado que devuelve la radicación, para las consultas posteriores |
| `documento` | El documento del ciudadano de prueba |
| `consecutivoIncidencia` | El consecutivo que devuelve el registro de incidencia |
| `rutaNueva`, `placaNueva`, `internoNuevo` | Identificadores únicos por ejecución |

---

## 3. Los datos de prueba

Viven en `src/main/resources/db/demo/V900__datos_demostracion.sql`, una ruta de migración que
**solo cargan los perfiles `local`, `dev` y `test`**. El perfil `prod` no la incluye.

### Usuarios

Las tres cuentas son las de los integrantes del equipo, con el rol que corresponde a la capa que
trabajó cada uno. El nombre de usuario es el de su correo institucional.

| Usuario | Persona | Contraseña | Rol |
|---|---|---|---|
| `rcarmonan` | Rosary Eilyn Carmona Navarro | `Transc@ribe2026.Adm` | ADMIN + FUNCIONARIO |
| `jarnedoz` | José Luis Arnedo Zambrano | `Transc@ribe2026.Fun` | FUNCIONARIO |
| `mortegas4` | Moisés Daniel Ortega Sánchez | `Transc@ribe2026.Aud` | AUDITOR |

### Flota y rutas

- Rutas `T100` y `T101`.
- Vehículos `UXT101`, `UXT102` (articulados), `UXP201`, `UXP202` (padrones), `UXA301`, `UXA302`
  (alimentadores). `UXP202` está en mantenimiento a propósito, para que haya un caso con estado
  distinto del habitual.
- Ciudadano de prueba con documento `1047398221`.

### Por qué no están en producción

Mantener cuentas de contraseña conocida fuera del camino de migración de producción cierra el
riesgo de desplegarlas sin darse cuenta. La contrapartida es que **en el despliegue publicado no
se puede iniciar sesión** y la colección completa no pasa contra él: sus peticiones de gestión
necesitan `jarnedoz` y la ruta `T100`, que allí no existen. El apartado 6 explica qué sí se puede
comprobar en la nube.

---

## 4. Las veintidós operaciones, una por una

Las veintidós operaciones del contrato están cubiertas. El rol es el que exige `@PreAuthorize`;
sin testigo, cualquiera de las protegidas responde `401`, y con un testigo de rol insuficiente,
`403`.

| Operación | Rol | Dato de prueba | Resultado esperado | Carpeta |
|---|---|---|---|---|
| `GET /catalogos/tipos-solicitud` | público | — | 200; la queja trae 15 días hábiles y su fundamento legal | 01 |
| `GET /catalogos/categorias` | público | — | 200; lista no vacía | 01 |
| `GET /catalogos/canales` | público | — | 200; incluye el canal `WEB` | 01 |
| `GET /rutas` | público | — | 200; rutas activas | 01 |
| `POST /pqrs` | público | Queja de la ruta `T100`, vehículo `UXT101` | 201; devuelve radicado, prioridad y vencimiento calculados por el servidor | 02 |
| `POST /pqrs` (denuncia) | público | Tipo `DENUNCIA`, categoría de seguridad | 201; prioridad elevada por la política, no por el cliente | 02 |
| `POST /pqrs` (inválida) | público | Descripción de dos caracteres | 400; el contrato rechaza el cuerpo | 02 |
| `POST /pqrs` (prioridad impuesta) | público | Cuerpo con `prioridad: CRITICA` | 400; campo no declarado (OWASP A08) | 02 |
| `GET /pqrs/publico/{radicado}` | público | Radicado **y** documento correctos | 200; datos enmascarados | 03 |
| `GET /pqrs/publico/{radicado}` | público | Documento ajeno | 404, el mismo que para un radicado inexistente | 03 |
| `PATCH /pqrs/{radicado}/estado` | — | Sin testigo | 401, no 403 | 03 |
| `POST /autenticacion/iniciar-sesion` | público | `jarnedoz` con su contraseña | 200; testigo JWT y roles | 04 |
| `POST /autenticacion/iniciar-sesion` | público | Contraseña incorrecta | 401 con mensaje genérico | 04 |
| `POST /autenticacion/iniciar-sesion` | público | Usuario inexistente | 401, **el mismo mensaje** | 04 |
| `GET /pqrs/{radicado}` | FUNCIONARIO, ADMIN, AUDITOR | Radicado de la carpeta 02 | 200; detalle completo sin enmascarar | 05 |
| `PATCH /pqrs/{radicado}/estado` | FUNCIONARIO, ADMIN | `EN_TRAMITE` con observación | 200; estado nuevo y seguimiento añadido | 05 |
| `PATCH /pqrs/{radicado}/estado` | FUNCIONARIO, ADMIN | Volver a `RADICADA` | 422; la máquina de estados lo rechaza | 05 |
| `POST /pqrs/{radicado}/respuesta` | FUNCIONARIO, ADMIN | Respuesta de fondo | 200; queda `RESUELTA` con fecha de respuesta | 05 |
| `GET /pqrs/{radicado}/seguimientos` | FUNCIONARIO, ADMIN, AUDITOR | Radicado ya tramitado | 200; la bitácora conserva las transiciones en orden | 05 |
| `GET /pqrs` | FUNCIONARIO, ADMIN, AUDITOR | Filtros por estado y página | 200; página con `totalElementos` | 05 |
| `GET /pqrs` (página abusiva) | FUNCIONARIO, ADMIN, AUDITOR | `tamano=5000` | 200; el servidor recorta el tamaño | 05 |
| `GET /vehiculos/{placa}` | FUNCIONARIO, ADMIN, AUDITOR | `UXT101` | 200; ficha del vehículo | 06 |
| `POST /incidencias` | FUNCIONARIO, ADMIN | Retraso en `UXT101`, vinculado al radicado | 201; nace `REPORTADA` y hereda prioridad de su tipo | 06 |
| `POST /incidencias` (futura) | FUNCIONARIO, ADMIN | Fecha en 2099 | 422; una incidencia no puede ocurrir mañana | 06 |
| `PATCH /incidencias/{c}/atencion` | FUNCIONARIO, ADMIN | Consecutivo de la anterior | 200; pasa a `EN_ATENCION` | 06 |
| `GET /incidencias` | FUNCIONARIO, ADMIN, AUDITOR | Filtro `placa=UXT101` | 200; página cuyas incidencias son todas de esa placa | 06 |
| `PATCH /incidencias/{c}/cierre` | FUNCIONARIO, ADMIN | Consecutivo en atención | 200; queda `CERRADA` sin perder el vínculo con la PQRS | 06 |
| `GET /indicadores` | ADMIN, AUDITOR | Testigo de funcionario | 403: autenticado pero sin ese rol | 07 |
| `GET /indicadores` | ADMIN, AUDITOR | Testigo de administrador | 200; cumplimiento entre 0 y 100 y desglose por estado | 07 |
| `POST /rutas` | ADMIN | Código `TZ…` generado en la ejecución | 201 con cabecera `Location`; nace activa | 08 |
| `POST /vehiculos` | ADMIN | Placa `UXZ…` articulada | 201 con `Location`; entra `OPERATIVO` con la capacidad de su tipo | 08 |
| `GET /vehiculos` | FUNCIONARIO, ADMIN, AUDITOR | Página de 50 ordenada por placa | 200; el vehículo recién vinculado ya figura | 08 |
| `POST /pqrs/{radicado}/asignacion` | ADMIN | `funcionario=jarnedoz` | 200; la solicitud queda a nombre de quien debe responderla | 08 |

---

## 5. Las pruebas que fallan a propósito

Son nueve, y son las que más dicen del sistema. Una suite que solo recorre caminos felices
demuestra que el programa hace algo; estas demuestran que **se niega a hacer lo que no debe**.

| Petición | Código | Regla que defiende |
|---|---|---|
| Radicación con descripción de dos caracteres | 400 | Validación de entrada estricta |
| Cuerpo con `prioridad` impuesta por el cliente | 400 | Asignación masiva de propiedades (OWASP A08:2021) |
| Consulta con documento ajeno | 404 | El radicado es consecutivo y adivinable; conocerlo no basta |
| Gestión sin testigo | 401 | 401 significa «identifíquese»; 403 significa «no le corresponde» |
| Contraseña incorrecta | 401 | Mensaje genérico: no revela si el usuario existe |
| Usuario inexistente | 401 | **El mismo** mensaje y un tiempo de respuesta comparable |
| Transición `RESUELTA → RADICADA` | 422 | La máquina de estados, y 422 porque es una regla de negocio, no un fallo |
| Incidencia fechada en 2099 | 422 | Un hecho operativo no puede ocurrir en el futuro |
| Indicadores con rol de funcionario | 403 | Autorización por método, no por ruta |

---

## 6. Contra el despliegue publicado

```bash
URL=https://transcaribe-pqrs-api.onrender.com

curl $URL/actuator/health                    # {"status":"UP"}
curl $URL/api/v1/catalogos/tipos-solicitud   # catálogo con los plazos legales
curl -i $URL/api/v1/indicadores              # 401: la autorización responde igual
curl -i $URL/                                # 401: la raíz tampoco es pública
```

Radicar y consultar también funcionan allí, sin `codigoRuta` porque en producción no hay rutas
cargadas todavía:

```bash
curl -X POST $URL/api/v1/pqrs -H "Content-Type: application/json" -d '{
  "ciudadano": {"tipoDocumento":"CC","numeroDocumento":"1047398221",
                "nombres":"Nombre","apellidos":"Apellido","correo":"correo@ejemplo.com"},
  "tipoSolicitud":"QUEJA","categoria":"SERVICIO_RUTA","canal":"WEB",
  "asunto":"Asunto de la prueba",
  "descripcion":"Descripcion suficientemente larga para pasar la validacion de entrada."}'

curl "$URL/api/v1/pqrs/publico/TC-2026-000001?documento=1047398221"
```

En la nube también se puede iniciar sesión, pero **no con estas credenciales**: el despliegue
tiene sus propias cuentas, creadas desde el entorno, con contraseñas que no están en ningún
archivo versionado. Lo que allí no existe es el catálogo de rutas y flota, que también es dato de
demostración; por eso la colección completa no pasa contra producción y el recorrido con datos se
hace en local.

---

## 7. Dónde queda la evidencia

| Archivo | Qué contiene |
|---|---|
| `docs/evidencias/postman-ejecucion.txt` | Salida literal de `newman run`, con el resumen de peticiones y aserciones |
| `docs/evidencias/docker-contenedores.txt` | Estado de los contenedores tras levantar la pila |
| `target/surefire-reports/` | Informe de JUnit por clase de prueba |
| `target/site/jacoco/index.html` | Cobertura de código |
| Pestaña *Actions* del repositorio | Las 168 pruebas ejecutadas en cada envío, con los informes como artefactos |
