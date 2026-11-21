package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import co.edu.unicartagena.transcaribe.pqrs.application.usecase.GestionarPqrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marca cada mañana las solicitudes que incumplieron el término legal.
 *
 * <p>El incumplimiento es un hecho del calendario: ocurre tanto si alguien
 * entra a consultarlo como si no. Calcularlo al vuelo en cada consulta daría
 * la misma cifra, pero no dejaría rastro del momento en que se incumplió, y
 * esa marca temporal es justo lo que un ente de control pide ver.</p>
 *
 * <p><b>Limitación conocida:</b> con varias instancias del servicio, todas
 * ejecutarían la tarea. Las transiciones son idempotentes —una solicitud ya
 * marcada no vuelve a cambiar de estado— así que no se corrompe nada, pero el
 * trabajo se duplica. Con más de una réplica correspondería un bloqueo
 * distribuido (ShedLock o equivalente); se deja anotado en trabajo futuro.</p>
 */
@Component
public class TareaVencimientoPqrs {

    private static final Logger LOG = LoggerFactory.getLogger(TareaVencimientoPqrs.class);

    private final GestionarPqrs gestionarPqrs;

    public TareaVencimientoPqrs(GestionarPqrs gestionarPqrs) {
        this.gestionarPqrs = gestionarPqrs;
    }

    @Scheduled(cron = "0 5 6 * * MON-FRI", zone = "America/Bogota")
    public void marcarVencidas() {
        try {
            int afectadas = gestionarPqrs.marcarVencidas();
            LOG.info("Revisión de vencimientos completada. Solicitudes marcadas: {}", afectadas);
        } catch (RuntimeException e) {
            LOG.error("Falló la revisión diaria de vencimientos", e);
        }
    }
}
