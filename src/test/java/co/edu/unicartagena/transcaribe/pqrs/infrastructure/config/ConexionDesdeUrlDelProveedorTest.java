package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import co.edu.unicartagena.transcaribe.pqrs.PqrsServiceApplication;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.mock.env.MockEnvironment;

/**
 * Pruebas de la traducción de la cadena de conexión del proveedor.
 *
 * <p>Render entrega la conexión como {@code postgresql://usuario:clave@servidor/base},
 * que es el convenio de los doce factores y no una URL JDBC. Traducirla mal no
 * produce un error claro, sino un intento de conexión a otro sitio, así que
 * cada pieza se verifica por separado.</p>
 */
@DisplayName("Conexión derivada de la cadena del proveedor")
class ConexionDesdeUrlDelProveedorTest {

    private static final String SIN_PUERTO =
            "postgresql://navilanchas:cl4v3@dpg-ejemplo-a/navilanchas";

    @Nested
    @DisplayName("al partir la cadena")
    class AlPartir {

        @Test
        @DisplayName("separa servidor, base, usuario y clave")
        void separaLasPiezas() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(SIN_PUERTO))
                    .containsEntry("DB_HOST", "dpg-ejemplo-a")
                    .containsEntry("DB_NAME", "navilanchas")
                    .containsEntry("DB_USER", "navilanchas")
                    .containsEntry("DB_PASSWORD", "cl4v3");
        }

        @Test
        @DisplayName("sin puerto explícito no inventa ninguno")
        void sinPuertoNoFijaNada() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(SIN_PUERTO))
                    .doesNotContainKey("DB_PORT");
        }

        @Test
        @DisplayName("con puerto explícito lo conserva")
        void conPuertoLoConserva() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(
                    "postgres://u:c@servidor.externo.com:6543/base"))
                    .containsEntry("DB_PORT", "6543")
                    .containsEntry("DB_HOST", "servidor.externo.com");
        }

        @Test
        @DisplayName("traslada los parámetros de consulta, que es donde viaja el TLS")
        void trasladaLosParametros() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(
                    "postgresql://u:c@servidor/base?sslmode=require"))
                    .containsEntry("DB_PARAMS", "?sslmode=require");
        }

        @Test
        @DisplayName("descodifica los caracteres escapados de la contraseña")
        void descodificaLaClave() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(
                    "postgresql://usuario:cl%40ve%2Fdif%C3%ADcil@servidor/base"))
                    .containsEntry("DB_PASSWORD", "cl@ve/difícil");
        }

        @Test
        @DisplayName("una URL JDBC no es suya: no la toca")
        void noTocaUnaUrlJdbc() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(
                    "jdbc:postgresql://servidor:5432/base")).isEmpty();
        }

        @Test
        @DisplayName("sin cadena no deriva nada")
        void sinCadenaNoDerivaNada() {
            assertThat(ConexionDesdeUrlDelProveedor.traducir(null)).isEmpty();
            assertThat(ConexionDesdeUrlDelProveedor.traducir("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("al decidir qué publicar")
    class AlPublicar {

        private static final UnaryOperator<String> NADA_DEFINIDO = clave -> null;

        @Test
        @DisplayName("publica todas las piezas cuando no hay nada definido")
        void publicaTodo() {
            assertThat(ConexionDesdeUrlDelProveedor.piezasPendientes(SIN_PUERTO, NADA_DEFINIDO))
                    .containsOnlyKeys("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD");
        }

        @Test
        @DisplayName("lo definido a mano manda sobre lo derivado")
        void loExplicitoGana() {
            Map<String, String> yaDefinidas = Map.of("DB_HOST", "otro-servidor");

            assertThat(ConexionDesdeUrlDelProveedor.piezasPendientes(SIN_PUERTO, yaDefinidas::get))
                    .doesNotContainKey("DB_HOST")
                    .containsEntry("DB_USER", "navilanchas");
        }

        @Test
        @DisplayName("el traductor se ejecuta antes que la comprobación")
        void seEjecutaAntesDeLaComprobacion() {
            assertThat(new ConexionDesdeUrlDelProveedor().getOrder())
                    .isLessThan(new VerificacionVariablesDespliegue().getOrder());
        }
    }

    @Nested
    @DisplayName("en el arranque real")
    class EnElArranque {

        private ApplicationEnvironmentPreparedEvent evento(MockEnvironment entorno) {
            return new ApplicationEnvironmentPreparedEvent(
                    new DefaultBootstrapContext(),
                    new SpringApplication(PqrsServiceApplication.class),
                    new String[0],
                    entorno);
        }

        @Test
        @DisplayName("con solo DATABASE_URL, producción arranca y la comprobación no se queja")
        void soloConLaCadenaDelProveedor() {
            MockEnvironment entorno = new MockEnvironment();
            entorno.setActiveProfiles("prod");
            entorno.setProperty(ConexionDesdeUrlDelProveedor.VARIABLE, SIN_PUERTO);

            new ConexionDesdeUrlDelProveedor().onApplicationEvent(evento(entorno));

            assertThat(entorno.getProperty("DB_HOST")).isEqualTo("dpg-ejemplo-a");
            assertThatCode(() -> new VerificacionVariablesDespliegue()
                    .onApplicationEvent(evento(entorno)))
                    .doesNotThrowAnyException();
        }
    }
}
