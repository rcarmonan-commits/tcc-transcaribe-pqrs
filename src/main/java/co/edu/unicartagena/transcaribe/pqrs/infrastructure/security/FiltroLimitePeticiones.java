package co.edu.unicartagena.transcaribe.pqrs.infrastructure.security;

import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita el número de peticiones por minuto y por origen.
 *
 * <p>Protege dos cosas distintas: el servicio frente a un abuso que lo agote, y
 * el formulario público de radicación frente a un envío masivo automatizado.
 * Sin este límite, el endpoint anónimo de radicación es un generador gratuito
 * de registros en la base de datos.</p>
 *
 * <p><b>Alcance honesto de esta implementación:</b> el contador vive en memoria
 * del proceso. Con una sola instancia es suficiente; con varias detrás de un
 * balanceador, cada una contaría por separado y el límite efectivo se
 * multiplicaría por el número de réplicas. La solución correcta en ese
 * escenario es un contador compartido (Redis) o el límite del propio pasarela
 * de entrada. Se documenta en el apartado de trabajo futuro en lugar de
 * fingir que esto resuelve el caso distribuido.</p>
 */
@Component
public class FiltroLimitePeticiones extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(FiltroLimitePeticiones.class);
    private static final long VENTANA_SEGUNDOS = 60L;
    private static final int MAXIMO_ORIGENES_VIGILADOS = 10_000;

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();
    private final boolean habilitado;
    private final int maximoPorMinuto;

    public FiltroLimitePeticiones(PropiedadesSeguridad propiedades) {
        this.habilitado = propiedades.limitePeticiones().habilitado();
        this.maximoPorMinuto = propiedades.limitePeticiones().maximoPorMinuto();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena) throws ServletException, IOException {

        if (!habilitado) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String origen = origenDe(peticion);
        if (excedeLimite(origen)) {
            LOG.warn("Límite de peticiones excedido para un origen; se rechaza la petición a {}",
                    peticion.getRequestURI());
            respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            respuesta.setHeader("Retry-After", String.valueOf(VENTANA_SEGUNDOS));
            respuesta.setContentType("application/json;charset=UTF-8");
            respuesta.getWriter().write(
                    "{\"codigo\":\"LIMITE_EXCEDIDO\",\"mensaje\":\"Demasiadas peticiones. Intente de nuevo en un minuto.\"}");
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    private boolean excedeLimite(String origen) {
        Instant ahora = Instant.now();

        // Poda sencilla para que el mapa no crezca sin límite ante un barrido
        // de direcciones falsificadas: sería una fuga de memoria aprovechable.
        if (contadores.size() > MAXIMO_ORIGENES_VIGILADOS) {
            contadores.entrySet().removeIf(e -> e.getValue().expirado(ahora));
        }

        Contador contador = contadores.compute(origen, (clave, actual) ->
                (actual == null || actual.expirado(ahora)) ? new Contador(ahora) : actual);

        return contador.incrementar() > maximoPorMinuto;
    }

    /**
     * Toma la dirección de la petición. Si hay un proxy de confianza delante,
     * {@code server.forward-headers-strategy} ya habrá normalizado la remota;
     * por eso no se lee {@code X-Forwarded-For} a mano, que es una cabecera que
     * cualquier cliente puede inventarse.
     */
    private String origenDe(HttpServletRequest peticion) {
        String remota = peticion.getRemoteAddr();
        return (remota == null || remota.isBlank()) ? "desconocido" : remota;
    }

    /** Ventana fija de un minuto con conteo atómico. */
    private static final class Contador {

        private final Instant inicio;
        private final AtomicInteger peticiones = new AtomicInteger(0);

        private Contador(Instant inicio) {
            this.inicio = inicio;
        }

        private int incrementar() {
            return peticiones.incrementAndGet();
        }

        private boolean expirado(Instant ahora) {
            return ahora.isAfter(inicio.plusSeconds(VENTANA_SEGUNDOS));
        }
    }
}
