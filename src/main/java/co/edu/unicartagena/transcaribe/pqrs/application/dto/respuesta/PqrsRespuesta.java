package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista completa de una solicitud, para el detalle que consulta el funcionario
 * o el propio ciudadano con su número de radicado.
 *
 * <p>No incluye el identificador interno de la fila: hacia afuera la solicitud
 * se identifica únicamente por el radicado. Un identificador secuencial
 * expuesto invita a recorrer solicitudes ajenas cambiando un número en la URL
 * (OWASP A01:2021, referencia directa insegura a objetos).</p>
 */
public record PqrsRespuesta(String radicado,
                            String tipoSolicitud,
                            String categoria,
                            String canal,
                            String estado,
                            String prioridad,
                            String asunto,
                            String descripcion,
                            String respuesta,
                            CiudadanoRespuesta ciudadano,
                            String codigoRuta,
                            String placaVehiculo,
                            String funcionarioAsignado,
                            LocalDateTime fechaRadicacion,
                            LocalDate fechaVencimiento,
                            LocalDateTime fechaRespuesta,
                            boolean vencida,
                            long diasRestantes,
                            String fundamentoLegal) {
}
