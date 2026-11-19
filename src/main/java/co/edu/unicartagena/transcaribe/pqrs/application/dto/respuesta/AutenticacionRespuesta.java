package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una autenticación correcta.
 *
 * <p>El testigo de acceso es de vida corta —treinta minutos por omisión— para
 * acotar la ventana de uso si alguien lo intercepta. No se emite testigo de
 * refresco: en un servicio de respaldo administrativo, volver a autenticarse es
 * aceptable y evita mantener una credencial de larga duración en el navegador.</p>
 */
public record AutenticacionRespuesta(String token,
                                     String tipo,
                                     String nombreUsuario,
                                     String nombreCompleto,
                                     List<String> roles,
                                     LocalDateTime expiraEn) {

    public static AutenticacionRespuesta bearer(String token,
                                                String nombreUsuario,
                                                String nombreCompleto,
                                                List<String> roles,
                                                LocalDateTime expiraEn) {
        return new AutenticacionRespuesta(token, "Bearer", nombreUsuario, nombreCompleto, roles, expiraEn);
    }
}
