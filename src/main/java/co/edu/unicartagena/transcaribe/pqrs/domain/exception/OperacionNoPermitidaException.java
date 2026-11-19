package co.edu.unicartagena.transcaribe.pqrs.domain.exception;

/** La operación es sintácticamente válida pero el estado del agregado la impide. */
public class OperacionNoPermitidaException extends ReglaNegocioException {

    public OperacionNoPermitidaException(String mensaje) {
        super("OPERACION_NO_PERMITIDA", mensaje);
    }
}
