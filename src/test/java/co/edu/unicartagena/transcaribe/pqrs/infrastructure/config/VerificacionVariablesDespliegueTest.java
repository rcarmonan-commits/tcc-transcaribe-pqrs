package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicartagena.transcaribe.pqrs.PqrsServiceApplication;
import java.util.List;
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
 * Pruebas de la comprobación de variables de despliegue.
 *
 * <p>Nacen de un fallo real: el servicio se publicó sin las credenciales de la
 * base y murió con «Connection to localhost:5432 refused». El mensaje era
 * cierto y, aun así, apuntaba al sitio equivocado: no faltaba una red, faltaba
 * una variable. Estas pruebas fijan que la ausencia se detecte antes de
 * intentar conectarse y se comunique como lo que es.</p>
 */
@DisplayName("Comprobación de variables de despliegue")
class VerificacionVariablesDespliegueTest {

    private static final Map<String, String> COMPLETAS = Map.of(
            "DB_HOST", "dpg-ejemplo-a",
            "DB_NAME", "transcaribe_pqrs",
            "DB_USER", "transcaribe",
            "DB_PASSWORD", "clave-de-ejemplo");

    private static UnaryOperator<String> lector(Map<String, String> valores) {
        return valores::get;
    }

    @Nested
    @DisplayName("al revisar qué falta")
    class AlRevisar {

        @Test
        @DisplayName("sin ninguna variable, las nombra todas")
        void sinNinguna() {
            assertThat(VerificacionVariablesDespliegue.ausentes(lector(Map.of())))
                    .containsExactly("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD");
        }

        @Test
        @DisplayName("con las cuatro definidas, no falta nada")
        void conLasCuatro() {
            assertThat(VerificacionVariablesDespliegue.ausentes(lector(COMPLETAS))).isEmpty();
        }

        @Test
        @DisplayName("una variable declarada pero vacía cuenta como ausente")
        void vaciaCuentaComoAusente() {
            Map<String, String> conHuecos = new java.util.HashMap<>(COMPLETAS);
            conHuecos.put("DB_HOST", "   ");

            assertThat(VerificacionVariablesDespliegue.ausentes(lector(conHuecos)))
                    .containsExactly("DB_HOST");
        }

        @Test
        @DisplayName("una cadena JDBC completa sustituye a las cuatro")
        void urlCompletaBasta() {
            Map<String, String> soloUrl = Map.of(
                    "DB_URL", "jdbc:postgresql://servidor:5432/transcaribe_pqrs");

            assertThat(VerificacionVariablesDespliegue.ausentes(lector(soloUrl))).isEmpty();
        }

        @Test
        @DisplayName("el mensaje nombra lo que falta y dónde se define")
        void elMensajeOrienta() {
            String mensaje = VerificacionVariablesDespliegue.mensaje(List.of("DB_HOST", "DB_USER"));

            assertThat(mensaje)
                    .contains("DB_HOST", "DB_USER")
                    .contains("localhost:5432")
                    .contains("Environment")
                    .contains("DB_URL");
        }
    }

    @Nested
    @DisplayName("al arrancar")
    class AlArrancar {

        private ApplicationEnvironmentPreparedEvent evento(MockEnvironment entorno) {
            return new ApplicationEnvironmentPreparedEvent(
                    new DefaultBootstrapContext(),
                    new SpringApplication(PqrsServiceApplication.class),
                    new String[0],
                    entorno);
        }

        @Test
        @DisplayName("en producción y sin credenciales, detiene el arranque")
        void produccionSinCredenciales() {
            MockEnvironment entorno = new MockEnvironment();
            entorno.setActiveProfiles("prod");

            assertThatThrownBy(() -> new VerificacionVariablesDespliegue()
                    .onApplicationEvent(evento(entorno)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_HOST");
        }

        @Test
        @DisplayName("fuera de producción no estorba, porque ahí las trae Compose")
        void fueraDeProduccionNoEstorba() {
            MockEnvironment entorno = new MockEnvironment();
            entorno.setActiveProfiles("dev");

            assertThatCode(() -> new VerificacionVariablesDespliegue()
                    .onApplicationEvent(evento(entorno)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("en producción y con credenciales, deja arrancar")
        void produccionConCredenciales() {
            MockEnvironment entorno = new MockEnvironment();
            entorno.setActiveProfiles("prod");
            COMPLETAS.forEach(entorno::setProperty);

            assertThatCode(() -> new VerificacionVariablesDespliegue()
                    .onApplicationEvent(evento(entorno)))
                    .doesNotThrowAnyException();
        }
    }
}
