package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

/** Ruta del sistema integrado tal como se publica en el API. */
public record RutaRespuesta(String codigo,
                            String nombre,
                            String origen,
                            String destino,
                            boolean activa) {
}
