# Despliegue en Render

Guía para publicar el servicio en Render usando el plano [`render.yaml`](../render.yaml).

El mismo `Dockerfile` que se usa en local con Compose es el que construye Render. Esa es la
razón de todo el montaje: lo que se verifica en la máquina es exactamente lo que se despliega,
sin una receta de construcción paralela que pueda divergir en silencio.

---

## 1. Qué crea el plano

| Recurso | Nombre | Plan |
|---|---|---|
| Base de datos PostgreSQL gestionada | `transcaribe-pqrs-bd` | Free |
| Servicio web, construido desde el Dockerfile | `transcaribe-pqrs-api` | Free |

Render conecta los dos: las credenciales de la base se inyectan al servicio como variables de
entorno. **Ninguna credencial se escribe en el repositorio ni queda dentro de la imagen.**

---

## 2. Procedimiento

1. Entrar a [dashboard.render.com](https://dashboard.render.com) y autenticarse con la cuenta
   de GitHub que tiene acceso al repositorio.
2. **New → Blueprint**.
3. Seleccionar el repositorio `rcarmonan-commits/tcc-transcaribe-pqrs`.
4. Render detecta `render.yaml` y muestra los dos recursos que va a crear. Confirmar con
   **Apply**.
5. Esperar. La primera construcción tarda entre ocho y doce minutos: descarga las dependencias
   de Maven, compila y arma la imagen en dos etapas.
6. Cuando el servicio quede en **Live**, copiar la URL que asigna Render
   (algo como `https://transcaribe-pqrs-api.onrender.com`).
7. **Ajustar `CORS_ORIGENES`** con esa URL real, en *Environment* del servicio, y guardar.
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
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `fromDatabase` | Render las toma de la base gestionada |
| `JWT_SECRET` | `generateValue: true` | La genera Render. Nadie la ve, ni siquiera quien despliega |
| `SPRING_PROFILES_ACTIVE` | fija: `prod` | Sin datos de demostración, trazas en WARN |
| `SWAGGER_ENABLED` | fija: `true` | Habilitada a propósito para que el API pueda explorarse |
| `CORS_ORIGENES` | fija, **ajustar tras desplegar** | Orígenes permitidos. No se admite `*` |
| `JAVA_OPTS` | fija | 70 % de la memoria disponible y recolector serie |
| `DB_POOL_MAX` | fija: `5` | El plan gratuito limita las conexiones simultáneas |
| `PORT` | la inyecta Render | La aplicación escucha en `${PORT:8080}` |

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
| El despliegue falla al arrancar y la traza menciona la clave de firma | `JWT_SECRET` no se generó | Verificar que la variable existe en *Environment*; si no, crearla con 48 caracteres aleatorios |
| Arranca y muere con error de conexión a la base | La base todavía no estaba lista | Render reintenta solo; si persiste, comprobar que ambos recursos están en la misma región |
| Hibernate se queja de una columna | El esquema y las entidades no coinciden | Revisar que la última migración se aplicó: la tabla `flyway_schema_history` lo dice |
| El navegador bloquea las peticiones por CORS | `CORS_ORIGENES` quedó con la URL de ejemplo | Ponerla con la URL real del servicio |
| La primera petición del día tarda un minuto | El servicio estaba dormido | Es el plan gratuito. Despertarlo antes de cualquier demostración |

---

## 6. Alternativa: desplegar sin el plano

Si prefiere crear los recursos a mano:

1. **New → PostgreSQL**, plan Free, región Oregon. Anotar servidor, puerto, base, usuario y
   clave.
2. **New → Web Service**, conectar el repositorio, *Runtime* **Docker**, rama `main`.
3. *Health Check Path*: `/actuator/health`.
4. Añadir a mano las variables de la tabla de la sección 3.
5. **Create Web Service**.

El resultado es el mismo; el plano solo evita escribir doce variables sin equivocarse.
