# =====================================================================
#  Imagen del servicio de PQRS — Transcaribe S.A.
#
#  Construcción en dos etapas. La primera compila; la segunda contiene
#  únicamente el entorno de ejecución. La diferencia no es estética: la
#  imagen final no lleva Maven, ni el código fuente, ni el repositorio de
#  dependencias, de modo que nada de eso puede filtrarse ni ser explotado
#  desde un contenedor en producción (OWASP A05:2021).
# =====================================================================

# ---------------------------------------------------------------------
# Etapa 1 — compilación
# ---------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS construccion

WORKDIR /construccion

# Las dependencias se resuelven antes de copiar el código: mientras el
# pom no cambie, esta capa se reutiliza y la construcción no vuelve a
# descargar el repositorio entero.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------
# Etapa 2 — ejecución
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Usuario sin privilegios: un proceso que no necesita ser root no debe
# serlo. Si alguien logra ejecución de código dentro del contenedor, se
# encuentra una cuenta sin permisos sobre el sistema de archivos.
RUN addgroup --system transcaribe && adduser --system --ingroup transcaribe transcaribe

WORKDIR /aplicacion

COPY --from=construccion --chown=transcaribe:transcaribe /construccion/target/*.jar aplicacion.jar

USER transcaribe

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

# Comprobación de salud: el orquestador necesita distinguir "arrancando"
# de "arrancado" para no enrutar tráfico a una instancia que aún no sirve.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar aplicacion.jar"]
