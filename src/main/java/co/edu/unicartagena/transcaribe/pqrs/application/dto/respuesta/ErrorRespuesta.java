package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cuerpo uniforme de error del API.
 *
 * <p>Deliberadamente no incluye traza de pila, nombre de clase ni mensaje del
 * motor de base de datos. Un error que revela la estructura interna le entrega
 * al atacante el mapa del sistema; es el escenario que OWASP describe en
 * A05:2021 sobre configuración de seguridad incorrecta.</p>
 *
 * @param codigo       identificador estable del error, apto para que el cliente lo interprete
 * @param mensaje      descripción legible, sin detalles internos
 * @param detalles     errores de validación campo a campo, cuando los hay
 * @param rastreo      identificador de correlación para buscar el evento en las trazas
 */
public record ErrorRespuesta(String codigo,
                             String mensaje,
                             Map<String, String> detalles,
                             List<String> errores,
                             String rastreo,
                             LocalDateTime instante) {

    public static ErrorRespuesta de(String codigo, String mensaje, String rastreo) {
        return new ErrorRespuesta(codigo, mensaje, null, null, rastreo, LocalDateTime.now());
    }

    public static ErrorRespuesta deValidacion(Map<String, String> detalles, String rastreo) {
        return new ErrorRespuesta("VALIDACION_FALLIDA",
                "La petición contiene campos inválidos",
                detalles, null, rastreo, LocalDateTime.now());
    }
}
