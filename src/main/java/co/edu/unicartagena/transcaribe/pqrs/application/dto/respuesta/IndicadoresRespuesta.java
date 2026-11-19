package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDate;
import java.util.Map;

/**
 * Indicadores de gestión del servicio de PQRS.
 *
 * <p>Son las cifras que la entidad no podía producir cuando el seguimiento
 * vivía en hojas de cálculo: cuántas solicitudes entraron, en qué estado están
 * y qué porcentaje se respondió dentro del término legal. El objetivo
 * específico OE-05 del proyecto se mide exactamente con este recurso.</p>
 *
 * @param cumplimientoOportunidad porcentaje de solicitudes resueltas dentro del
 *                                término legal, con dos decimales
 */
public record IndicadoresRespuesta(LocalDate desde,
                                   LocalDate hasta,
                                   long totalRadicadas,
                                   Map<String, Long> porEstado,
                                   long resueltasEnPlazo,
                                   long resueltasFueraDePlazo,
                                   double cumplimientoOportunidad,
                                   long vencidasSinRespuesta,
                                   long incidenciasDelAnio) {
}
