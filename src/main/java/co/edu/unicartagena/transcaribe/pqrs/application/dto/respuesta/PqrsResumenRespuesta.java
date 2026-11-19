package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista reducida para listados y tableros.
 *
 * <p>Los datos del ciudadano salen enmascarados. Un listado se consulta muchas
 * veces y se cachea con facilidad; no hay razón para que arrastre documentos
 * completos cuando el funcionario solo necesita reconocer la solicitud. Es el
 * principio de minimización de datos aplicado a una respuesta JSON.</p>
 */
public record PqrsResumenRespuesta(String radicado,
                                   String tipoSolicitud,
                                   String categoria,
                                   String estado,
                                   String prioridad,
                                   String asunto,
                                   String ciudadanoDocumentoEnmascarado,
                                   LocalDateTime fechaRadicacion,
                                   LocalDate fechaVencimiento,
                                   boolean vencida) {
}
