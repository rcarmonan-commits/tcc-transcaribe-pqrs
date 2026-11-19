package co.edu.unicartagena.transcaribe.pqrs.domain.exception;

/**
 * Raíz de las excepciones de dominio.
 *
 * <p>El dominio no conoce HTTP: lanza esta jerarquía y es la capa web quien
 * decide el código de respuesta. Así la regla de negocio puede reutilizarse
 * desde un proceso programado o desde una cola de mensajes sin arrastrar
 * dependencias del framework (inversión de dependencias).</p>
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
