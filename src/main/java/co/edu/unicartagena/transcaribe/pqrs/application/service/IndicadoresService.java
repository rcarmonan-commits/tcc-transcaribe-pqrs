package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.IndicadoresRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.IncidenciaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores de gestión del servicio (CU-10).
 *
 * <p>Responde a la pregunta que la entidad no podía contestar con el registro
 * en hojas de cálculo: qué porcentaje de las solicitudes se respondió dentro
 * del término legal. Es el indicador con el que se verifica el objetivo
 * específico OE-05 del proyecto.</p>
 */
@Service
public class IndicadoresService {

    private final PqrsRepositoryPort pqrsRepositorio;
    private final IncidenciaRepositoryPort incidenciaRepositorio;
    private final Reloj reloj;

    public IndicadoresService(PqrsRepositoryPort pqrsRepositorio,
                              IncidenciaRepositoryPort incidenciaRepositorio,
                              Reloj reloj) {
        this.pqrsRepositorio = pqrsRepositorio;
        this.incidenciaRepositorio = incidenciaRepositorio;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public IndicadoresRespuesta calcular(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = reloj.hoy();
        LocalDate inicio = desde != null ? desde : hoy.withDayOfYear(1);
        LocalDate fin = hasta != null ? hasta : hoy;

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (CodigoEstadoPqrs estado : CodigoEstadoPqrs.values()) {
            porEstado.put(estado.name(), pqrsRepositorio.contarPorEstado(estado.name()));
        }

        long enPlazo = pqrsRepositorio.contarResueltasDentroDePlazo();
        long fueraDePlazo = pqrsRepositorio.contarResueltasFueraDePlazo();

        return new IndicadoresRespuesta(
                inicio,
                fin,
                pqrsRepositorio.contarRadicadasEntre(inicio, fin),
                porEstado,
                enPlazo,
                fueraDePlazo,
                porcentajeCumplimiento(enPlazo, fueraDePlazo),
                pqrsRepositorio.buscarVencidasSinRespuesta(hoy).size(),
                incidenciaRepositorio.contarDelAnio(hoy.getYear()));
    }

    /**
     * Porcentaje de oportunidad con dos decimales.
     *
     * <p>Se usa {@link BigDecimal} y no aritmética de coma flotante porque este
     * número acaba en un informe de gestión: un 99.99999 por redondeo binario
     * es un error que alguien tendría que explicar.</p>
     */
    private double porcentajeCumplimiento(long enPlazo, long fueraDePlazo) {
        long total = enPlazo + fueraDePlazo;
        if (total == 0L) {
            return 0.0d;
        }
        return BigDecimal.valueOf(enPlazo)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
