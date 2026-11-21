package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Festivo;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabil;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabilColombiano;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantiene en memoria el calendario de festivos y lo refresca a diario.
 *
 * <p>Consultar la tabla de festivos en cada radicación sería un viaje a la
 * base de datos para leer treinta y seis filas que cambian una vez al año. Por
 * otro lado, cargarlas solo al arrancar obligaría a reiniciar el servicio para
 * corregir un festivo. El refresco diario resuelve ambos extremos.</p>
 *
 * <p>La referencia al calendario es {@code volatile} y se sustituye entera en
 * cada refresco: así ningún hilo llega a ver un conjunto a medio construir.</p>
 */
@Component
public class ProveedorCalendarioHabil implements CalendarioHabil {

    private static final Logger LOG = LoggerFactory.getLogger(ProveedorCalendarioHabil.class);

    private final CatalogoRepositoryPort catalogo;
    private volatile CalendarioHabilColombiano delegado = new CalendarioHabilColombiano(Set.of());

    public ProveedorCalendarioHabil(CatalogoRepositoryPort catalogo) {
        this.catalogo = catalogo;
    }

    @PostConstruct
    @Scheduled(cron = "0 15 3 * * *", zone = "America/Bogota")
    public void refrescar() {
        try {
            Set<LocalDate> festivos = catalogo.listarFestivos().stream()
                    .map(Festivo::getFecha)
                    .collect(Collectors.toUnmodifiableSet());

            this.delegado = new CalendarioHabilColombiano(festivos);
            LOG.info("Calendario de días hábiles cargado: {} festivos", festivos.size());
        } catch (RuntimeException e) {
            // Se conserva el calendario anterior: un fallo de lectura no debe
            // dejar el servicio calculando plazos sin festivos, que daría
            // vencimientos más cortos de lo que permite la ley.
            LOG.error("No se pudo refrescar el calendario de festivos; se conserva el anterior", e);
        }
    }

    @Override
    public boolean esHabil(LocalDate fecha) {
        return delegado.esHabil(fecha);
    }

    @Override
    public LocalDate sumarDiasHabiles(LocalDate desde, int diasHabiles) {
        return delegado.sumarDiasHabiles(desde, diasHabiles);
    }

    @Override
    public long diasHabilesEntre(LocalDate desde, LocalDate hasta) {
        return delegado.diasHabilesEntre(desde, hasta);
    }
}
