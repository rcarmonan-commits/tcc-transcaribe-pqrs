package co.edu.unicartagena.transcaribe.pqrs.domain.exception;

/** Viola una restricción de unicidad del modelo (documento, placa, radicado). */
public class RecursoDuplicadoException extends ReglaNegocioException {

    public RecursoDuplicadoException(String recurso, Object identificador) {
        super("RECURSO_DUPLICADO",
              "Ya existe %s con identificador %s".formatted(recurso, identificador));
    }
}
