package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Traduce la cadena de conexión del proveedor a las piezas que espera el driver.
 *
 * <p>Render, Heroku, Railway y Neon entregan la conexión en una sola variable,
 * {@code DATABASE_URL}, con la forma
 * {@code postgresql://usuario:clave@servidor:puerto/base}. Es el convenio de la
 * tercera regla de los doce factores, y tiene una ventaja práctica: el
 * proveedor puede inyectarla desde la propia base de datos, de modo que nadie
 * teclea ni versiona una credencial.</p>
 *
 * <p>El inconveniente es que **no es una URL JDBC**. El driver de PostgreSQL
 * para Java espera {@code jdbc:postgresql://servidor:puerto/base} con el
 * usuario y la clave aparte, así que entregarle la cadena del proveedor tal
 * cual falla. Esta clase deshace esa diferencia: parte la cadena y publica
 * {@code DB_HOST}, {@code DB_PORT}, {@code DB_NAME}, {@code DB_USER},
 * {@code DB_PASSWORD} y {@code DB_PARAMS} antes de que nada las lea.</p>
 *
 * <p>Una pieza que ya venga definida por su cuenta **no se toca**. Lo explícito
 * gana sobre lo derivado: quien se molestó en fijar {@code DB_HOST} a mano
 * quería ese servidor, no el que figure en la cadena.</p>
 *
 * <p>Se ejecuta antes que {@link VerificacionVariablesDespliegue}, para que la
 * comprobación vea ya las piezas traducidas.</p>
 */
public class ConexionDesdeUrlDelProveedor
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    /** Nombre de la variable en Render, Heroku, Railway y Neon. */
    static final String VARIABLE = "DATABASE_URL";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
        ConfigurableEnvironment entorno = evento.getEnvironment();

        Map<String, Object> derivadas = piezasPendientes(
                entorno.getProperty(VARIABLE), entorno::getProperty);

        if (!derivadas.isEmpty()) {
            entorno.getPropertySources().addFirst(
                    new MapPropertySource("conexion-derivada-de-" + VARIABLE, derivadas));
        }
    }

    /**
     * Piezas que hay que publicar: las que salen de la cadena y todavía no
     * están definidas por otra vía.
     *
     * @param url    la cadena del proveedor, o {@code null}
     * @param lector lee una variable ya definida; devuelve {@code null} si no lo está
     */
    static Map<String, Object> piezasPendientes(String url, UnaryOperator<String> lector) {
        Map<String, Object> piezas = new LinkedHashMap<>(traducir(url));
        piezas.keySet().removeIf(clave -> tieneValor(lector.apply(clave)));
        return piezas;
    }

    /**
     * Parte la cadena del proveedor en sus componentes.
     *
     * <p>Devuelve un mapa vacío si la cadena está ausente o no es del formato
     * del proveedor: una URL JDBC ya servida en {@code DB_URL} no se toca.</p>
     */
    static Map<String, Object> traducir(String url) {
        if (!tieneValor(url)) {
            return Map.of();
        }
        String limpia = url.trim();
        if (!limpia.startsWith("postgres://") && !limpia.startsWith("postgresql://")) {
            return Map.of();
        }

        URI uri = URI.create(limpia);
        String servidor = uri.getHost();
        if (!tieneValor(servidor)) {
            return Map.of();
        }

        Map<String, Object> piezas = new LinkedHashMap<>();
        piezas.put("DB_HOST", servidor);

        // Sin puerto explícito no se inventa uno: así una variable DB_PORT
        // definida aparte sigue mandando, y si tampoco existe queda el 5432
        // por omisión del perfil.
        if (uri.getPort() > 0) {
            piezas.put("DB_PORT", String.valueOf(uri.getPort()));
        }

        String base = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (tieneValor(base)) {
            piezas.put("DB_NAME", base);
        }

        String credenciales = uri.getUserInfo();
        if (tieneValor(credenciales)) {
            int separador = credenciales.indexOf(':');
            if (separador < 0) {
                piezas.put("DB_USER", descodificar(credenciales));
            } else {
                piezas.put("DB_USER", descodificar(credenciales.substring(0, separador)));
                piezas.put("DB_PASSWORD", descodificar(credenciales.substring(separador + 1)));
            }
        }

        if (tieneValor(uri.getQuery())) {
            piezas.put("DB_PARAMS", "?" + uri.getQuery());
        }

        return piezas;
    }

    private static String descodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
