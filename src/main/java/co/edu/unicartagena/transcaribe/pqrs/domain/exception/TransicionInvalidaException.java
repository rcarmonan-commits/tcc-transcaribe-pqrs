package co.edu.unicartagena.transcaribe.pqrs.domain.exception;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;

/** Intento de mover una PQRS a un estado no alcanzable desde el actual (RN-04). */
public class TransicionInvalidaException extends ReglaNegocioException {

    public TransicionInvalidaException(CodigoEstadoPqrs origen, CodigoEstadoPqrs destino) {
        super("TRANSICION_INVALIDA",
              "No se permite pasar de %s a %s".formatted(origen, destino));
    }
}
