package co.edu.unicartagena.transcaribe.pqrs.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fuente de tiempo del dominio.
 *
 * <p>Ninguna regla llama a {@code LocalDateTime.now()} directamente. Una regla
 * que consulta el reloj del sistema no se puede probar: el resultado depende
 * del momento en que corra la prueba. Inyectar el tiempo convierte "la
 * solicitud venció" en una afirmación verificable.</p>
 */
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
