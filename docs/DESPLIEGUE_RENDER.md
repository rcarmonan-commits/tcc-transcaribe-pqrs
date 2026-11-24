# Despliegue en Render

Guía para publicar el servicio en Render usando el plano [`render.yaml`](../render.yaml).

El mismo `Dockerfile` que se usa en local con Compose es el que construye Render. Esa es la
razón de todo el montaje: lo que se verifica en la máquina es exactamente lo que se despliega,
sin una receta de construcción paralela que pueda divergir en silencio.

---

## 1. Qué crea el plano, y qué no

| Recurso | Nombre | Plan |
|---|---|---|
| Servicio web, construido desde el Dockerfile | `transcaribe-pqrs-api` | Free |

**El plano no crea la base de datos.** Render admite **una sola base PostgreSQL gratuita por
cuenta**: si ya hay otra activa, el despliegue falla con

```
cannot have more than one active free tier database
```

y cancela el servicio entero.

La salida no es borrar la base que ya existe —puede estar sirviendo a otro proyecto— sino
**compartirla sin mezclarse**. El servicio crea su propio esquema dentro de ella, declarado en la
variable `DB_ESQUEMA`, y no ve ni toca las tablas de nadie más:

```
base compartida
├── public        <- tablas del otro proyecto, intactas
└── pqrs          <- las 19 tablas de este servicio + flyway_schema_history
```

Flyway crea el esquema al arrancar (`create-schemas: true`) e Hibernate valida contra él
(`default_schema`). Está verificado: con una base que ya tenía tablas en `public`, el servicio
levantó, creó sus veinte tablas en `pqrs` y dejó `public` sin tocar.

Las credenciales se piden al aplicar el plano, de modo que sirven igual para una base de Render
que para cualquier PostgreSQL externo. **Ninguna credencial se escribe en el repositorio ni queda
dentro de la imagen.**

---

## 2. Procedimiento

1. Entrar a [dashboard.render.com](https://dashboard.render.com) y autenticarse con la cuenta
   de GitHub que tiene acceso al repositorio.
2. Abrir la base PostgreSQL que ya exista en la cuenta y, en su pestaña **Connections**, anotar
   **Hostname**, **Database**, **Username** y **Password**. Si la base está en Render, use el
   *Internal Database URL*: el host interno no sale a internet.
3. **New → Blueprint**.
4. Seleccionar el repositorio `rcarmonan-commits/tcc-transcaribe-pqrs`.
5. Render detecta `render.yaml` y **pide los cuatro valores** del paso 2: `DB_HOST`, `DB_NAME`,
   `DB_USER` y `DB_PASSWORD`. Rellenarlos y confirmar con **Apply**.

   Si la base es externa a Render, o se conecta por su host público, poner además
   `DB_PARAMS` en `?sslmode=require`.
6. Esperar. La primera construcción tarda entre ocho y doce minutos: descarga las dependencias
   de Maven, compila y arma la imagen en dos etapas.
7. Cuando el servicio quede en **Live**, copiar la URL que asigna Render
   (algo como `https://transcaribe-pqrs-api.onrender.com`).
8. **Ajustar `CORS_ORIGENES`** con esa URL real, en *Environment* del servicio, y guardar.
   Render redespliega solo.

### Comprobación

```bash
URL=https://transcaribe-pqrs-api.onrender.com

curl $URL/actuator/health
# {"status":"UP"}

curl $URL/api/v1/catalogos/tipos-solicitud
# el catálogo con los plazos legales

curl -i $URL/api/v1/indicadores
# 401: la autorización funciona igual que en local
```

Para probar el flujo completo, cambie la variable `baseUrl` de la colección de Postman por esa
URL y ejecute el Runner. Las 73 aserciones deben pasar igual que contra el despliegue local.

---

## 3. Variables de entorno

Las gestiona el plano; esta tabla es para entender qué hace cada una, no para escribirlas a
mano.

| Variable | Origen | Para qué |
|---|---|---|
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | se piden al desplegar | Credenciales de la base que ya existe en la cuenta |
| `DB_ESQUEMA` | fija: `pqrs` | Espacio de nombres propio dentro de esa base |
| `DB_PARAMS` | vacía | `?sslmode=require` si la base es externa o se conecta por host público |
| `JWT_SECRET` | `generateValue: true` | La genera Render. Nadie la ve, ni siquiera quien despliega |
| `SPRING_PROFILES_ACTIVE` | fija: `prod` | Sin datos de demostración, trazas en WARN |
| `SWAGGER_ENABLED` | fija: `true` | Habilitada a propósito para que el API pueda explorarse |
| `CORS_ORIGENES` | fija, **ajustar tras desplegar** | Orígenes permitidos. No se admite `*` |
| `JAVA_OPTS` | fija | 70 % de la memoria disponible y recolector serie |
| `DB_POOL_MAX` | fija: `5` | El plan gratuito limita las conexiones simultáneas |
| `PORT` | la inyecta Render | La aplicación escucha en `${PORT:8080}` |

### Qué pasa si una de las cuatro no llega

El servicio **no arranca**, y lo dice con esas palabras:

```
El perfil de producción no puede arrancar: falta definir DB_HOST, DB_NAME, DB_USER,
DB_PASSWORD. Sin esas variables la aplicación compondría una URL hacia localhost:5432,
donde no hay ninguna base de datos...
```

La comprobación existe porque sin ella el fallo era peor que silencioso: era engañoso. Cada
pieza de la URL tiene un valor por omisión, así que una variable ausente no producía un error
de configuración sino una URL válida apuntando a `localhost`. Dentro del contenedor ahí no hay
nada, y el arranque moría sesenta líneas después con un `Connection to localhost:5432 refused`
que parece un problema de red. Quien lo lee busca un cortafuegos cuando lo que falta es un
valor. La verificación corre al preparar el entorno, antes de que Flyway o el grupo de
conexiones existan, y está cubierta por ocho pruebas.

### Por qué la URL de la base se arma por piezas

Render entrega su cadena de conexión en formato `postgres://usuario:clave@servidor/base`, que
**no** es una URL JDBC: el driver de Java espera `jdbc:postgresql://servidor:puerto/base` con
el usuario y la clave aparte. Por eso el perfil de producción compone la URL a partir de
`DB_HOST`, `DB_PORT` y `DB_NAME`:

```yaml
url: ${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:transcaribe_pqrs}}
```

Se admite además `DB_URL` completa, de modo que el mismo artefacto sirve para otro proveedor
que sí entregue una URL JDBC.

---

## 4. Limitaciones del plan gratuito

Conviene conocerlas antes de la sustentación, para que nada sorprenda en vivo.

- **El servicio se duerme tras quince minutos sin tráfico.** La primera petición después de
  ese reposo tarda entre treinta y cincuenta segundos en responder, porque el contenedor
  vuelve a arrancar. **Antes de grabar o sustentar, haga una petición para despertarlo** y
  espere a que responda.
- **La base gratuita caduca a los treinta días.** Anote la fecha de creación. Si la entrega es
  posterior, habrá que recrearla; el esquema se vuelve a aplicar solo porque lo gobierna
  Flyway.
- **512 MB de memoria y un núcleo compartido.** Suficiente para la demostración; no para carga
  real.
- **El disco es efímero.** No importa aquí: el estado vive en PostgreSQL, no en el sistema de
  archivos del contenedor.

---

## 5. Si algo falla

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `cannot have more than one active free tier database` | La cuenta ya tiene una base gratuita | Es lo previsto: este plano no crea base. Use la existente con su propio `DB_ESQUEMA` |
| Arranca pero no encuentra las tablas | `DB_ESQUEMA` cambió entre despliegues | Flyway creó el esquema anterior; vuelva a poner el mismo valor |
| El arranque se detiene diciendo que falta `DB_HOST`, `DB_NAME`, `DB_USER` o `DB_PASSWORD` | Se aplicó el plano sin rellenar los valores que pide | Añadirlos en *Environment* del servicio con los datos del paso 2. El servicio los comprueba antes de intentar conectarse, a propósito |
| El despliegue falla al arrancar y la traza menciona la clave de firma | `JWT_SECRET` no se generó | Verificar que la variable existe en *Environment*; si no, crearla con 48 caracteres aleatorios |
| Arranca y muere con error de conexión a la base | El servidor o la clave no son los correctos | Volver a copiarlos de *Connections*. Si la base está en otra región, usar el host externo y poner `DB_PARAMS` en `?sslmode=require` |
| Hibernate se queja de una columna | El esquema y las entidades no coinciden | Revisar que la última migración se aplicó: la tabla `flyway_schema_history` lo dice |
| El navegador bloquea las peticiones por CORS | `CORS_ORIGENES` quedó con la URL de ejemplo | Ponerla con la URL real del servicio |
| La primera petición del día tarda un minuto | El servicio estaba dormido | Es el plan gratuito. Despertarlo antes de cualquier demostración |

---

## 6. Alternativa: desplegar sin el plano

Si prefiere crear los recursos a mano:

1. **New → PostgreSQL**, plan Free, región Oregon. Anotar servidor, puerto, base, usuario y
   clave.
2. **New → Web Service**, conectar el repositorio, *Runtime* **Docker**, rama `main`.
   (Omita el paso 1 si ya tiene una base: reutilícela con un `DB_ESQUEMA` propio.)
3. *Health Check Path*: `/actuator/health`.
4. Añadir a mano las variables de la tabla de la sección 3.
5. **Create Web Service**.

El resultado es el mismo; el plano solo evita escribir doce variables sin equivocarse.
