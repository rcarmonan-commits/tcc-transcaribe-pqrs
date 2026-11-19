package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDateTime;

/** Entrada de la bitácora de una solicitud, tal como se publica en el API. */
public record SeguimientoRespuesta(String estadoAnterior,
                                   String estadoNuevo,
                                   String observacion,
                                   String funcionario,
                                   LocalDateTime fechaRegistro) {
}
