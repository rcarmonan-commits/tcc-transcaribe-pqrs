package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDateTime;

/**
 * Datos de un ciudadano devueltos por el API.
 *
 * <p>Existe un DTO separado de la entidad por una razón concreta: la entidad
 * guarda el documento y el correo completos, y esta vista decide cuánto de eso
 * sale del servidor. Serializar la entidad directamente publicaría todo el
 * estado interno, incluidas las relaciones, sin que nadie lo decidiera.</p>
 */
public record CiudadanoRespuesta(Long id,
                                 String tipoDocumento,
                                 String numeroDocumento,
                                 String nombres,
                                 String apellidos,
                                 String correo,
                                 String telefono,
                                 LocalDateTime fechaRegistro) {
}
