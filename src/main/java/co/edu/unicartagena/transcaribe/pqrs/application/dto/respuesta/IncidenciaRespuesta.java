package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDateTime;

/** Incidencia de flota tal como se publica en el API. */
public record IncidenciaRespuesta(String consecutivo,
                                  String placaVehiculo,
                                  String codigoRuta,
                                  String tipoIncidencia,
                                  String prioridad,
                                  String estado,
                                  String descripcion,
                                  String radicadoAsociado,
                                  LocalDateTime fechaHora) {
}
