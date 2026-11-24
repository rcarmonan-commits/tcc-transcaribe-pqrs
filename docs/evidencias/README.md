# Evidencias de verificación

Salidas reales de la verificación del sistema. No son capturas de pantalla ni
transcripciones: son la salida literal de las herramientas, guardada para que
cualquiera pueda contrastarla volviendo a ejecutar los mismos comandos.

## Suite de pruebas automatizadas

```bash
./mvnw verify
```

Resultado al cierre del cuarto sprint: **168 pruebas, 0 fallos, 0 errores**.
El informe completo de Surefire y el de cobertura de JaCoCo quedan en `target/` y los
publica también la integración continua como artefactos descargables de cada ejecución.

| Nivel | Pruebas | Qué verifica |
|---|---|---|
| Dominio | 105 | Reglas de negocio aisladas, sin framework ni base de datos |
| Aplicación | 30 | Casos de uso con los puertos sustituidos por dobles |
| Infraestructura | 25 | Configuración de despliegue: credenciales ausentes y traducción de la cadena de conexión |
| Integración | 8 | Flujo completo del API sobre H2 con el esquema real de Flyway |

## Despliegue en Docker

`docker-contenedores.txt` recoge el estado de los contenedores tras levantar la pila
completa con `docker compose up --build`:

- `transcaribe-pqrs-api` — el servicio, imagen construida en dos etapas sobre
  `eclipse-temurin:21-jre-alpine`, con usuario sin privilegios y sistema de archivos de
  solo lectura. Estado **healthy**, confirmado por la comprobación de salud del propio
  contenedor contra `/actuator/health`.
- `transcaribe-pqrs-bd` — PostgreSQL 16, con el puerto enlazado solo a `127.0.0.1` y el
  esquema aplicado por Flyway al arrancar el servicio.

## Verificación del API con Postman

`postman-ejecucion.txt` es la salida de ejecutar la colección completa **contra el
despliegue en Docker**, no contra un servicio de pruebas:

```bash
newman run postman/Transcaribe-PQRS.postman_collection.json
```

| Métrica | Resultado |
|---|---|
| Peticiones ejecutadas | 27 |
| Aserciones | 73 |
| Fallos | **0** |
| Duración total | 3,8 s |
| Tiempo de respuesta medio | 61 ms |

El tiempo medio de 61 ms y el máximo de 281 ms cumplen con holgura el requisito RNF-01,
que exige respuestas por debajo de dos segundos.

La colección no comprueba solo los caminos felices. Entre las 95 aserciones hay
verificaciones de que el sistema **rechaza** lo que debe rechazar:

- radicación con campos inválidos → `400` con el detalle campo a campo;
- intento de fijar la prioridad desde el cliente → `400`, el contrato no admite campos
  no declarados;
- consulta con el documento de otra persona → `404`, el mismo error que para un radicado
  inexistente;
- gestión sin credenciales → `401` con código `NO_AUTENTICADO`;
- transición de estado ilegal → `422` con código `TRANSICION_INVALIDA`;
- indicadores con rol de funcionario → `403`, hace falta administrador o auditoría;
- incidencia fechada en el futuro → `422`;
- tamaño de página de 100.000 → recortado a 100 por el servidor.

## Integración continua

El flujo de GitHub Actions ejecuta en cada empuje la compilación, la suite completa y la
construcción de la imagen Docker, y publica los informes como artefactos. Sobre la rama
principal ejecuta además el análisis de dependencias vulnerables con OWASP
Dependency-Check.

Estado de la última ejecución sobre `main`: **los tres trabajos en verde**.

> Nota de proceso: la primera ejecución falló porque el wrapper de Maven se había
> versionado sin permiso de ejecución, algo que no se nota trabajando en Windows y que
> rompe cualquier flujo que corra en Linux. Se corrigió con
> `git update-index --chmod=+x mvnw`. Queda anotado aquí porque es exactamente el tipo de
> fallo que la integración continua existe para encontrar.
