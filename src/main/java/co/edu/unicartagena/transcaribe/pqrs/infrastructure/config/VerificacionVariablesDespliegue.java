package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import java.util.List;
import java.util.function.UnaryOperator;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Comprueba, antes de construir un solo objeto, que el perfil de producción
 * tiene las variables con las que conectarse a la base de datos.
 *
 * <p>Sin esta comprobación el arranque no falla donde debería. El perfil de
 * producción compone la URL de conexión a partir de piezas sueltas y cada una
 * tiene un valor por omisión, de modo que una variable ausente no produce un
 * error de configuración sino una URL perfectamente válida que apunta a
 * {@code localhost}. Dentro de un contenedor ahí no hay ninguna base, así que
 * el fallo llega sesenta líneas después, disfrazado de problema de red:
 * <em>connection refused</em>. Quien lo lee busca un cortafuegos cuando lo que
 * falta es un valor.</p>
 *
 * <p>Esa es justamente la situación que el perfil local evita declarando las
 * variables con la sintaxis que rompe el arranque si faltan. Producción
 * sostiene aquí el mismo criterio: una credencial ausente debe detener el
 * servicio con un mensaje que diga qué falta y dónde se pone, no dejarlo
 * arrancar a medias ni confundir a quien despliega.</p>
 *
 * <p>Se registra en {@code PqrsServiceApplication} y se ejecuta al preparar el
 * entorno, antes de que Flyway o el repositorio de conexiones existan.</p>
 */
public class VerificacionVariablesDespliegue
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    /** Perfil en el que la comprobación tiene sentido. */
    static final String PERFIL_VIGILADO = "prod";

    /** Alternativa aceptada: una cadena JDBC completa en una sola variable. */
    static final String VARIABLE_ALTERNATIVA = "DB_URL";

    static final List<String> OBLIGATORIAS = List.of("DB_HOST", "DB_NAME", "DB_USER", "DB_PASSWORD");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
        ConfigurableEnvironment entorno = evento.getEnvironment();

        if (!List.of(entorno.getActiveProfiles()).contains(PERFIL_VIGILADO)) {
            return;
        }

        List<String> ausentes = ausentes(entorno::getProperty);
        if (!ausentes.isEmpty()) {
            throw new IllegalStateException(mensaje(ausentes));
        }
    }

    /**
     * Variables que faltan para poder conectarse.
     *
     * <p>Se expone con visibilidad de paquete y recibe un lector en lugar del
     * entorno de Spring para poder probarse sin levantar un contexto.</p>
     *
     * @param lector devuelve el valor de una variable, o {@code null}
     * @return las obligatorias que faltan; vacía si hay con qué conectarse
     */
    static List<String> ausentes(UnaryOperator<String> lector) {
        if (tieneValor(lector.apply(VARIABLE_ALTERNATIVA))) {
            return List.of();
        }
        return OBLIGATORIAS.stream()
                .filter(variable -> !tieneValor(lector.apply(variable)))
                .toList();
    }

    /** Una variable declarada pero vacía no sirve: cuenta como ausente. */
    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    static String mensaje(List<String> ausentes) {
        return "El perfil de producción no puede arrancar: falta definir "
                + String.join(", ", ausentes)
                + ". Sin esas variables la aplicación compondría una URL hacia localhost:5432, "
                + "donde no hay ninguna base de datos, y el error aparecería como «connection "
                + "refused» en vez de como lo que es: configuración ausente. "
                + "Defínalas en el proveedor con los datos de conexión de la base —en Render, "
                + "en la pestaña Environment del servicio— o entregue la cadena JDBC completa "
                + "en " + VARIABLE_ALTERNATIVA + ".";
    }
}
