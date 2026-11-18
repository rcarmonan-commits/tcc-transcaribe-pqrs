package co.edu.unicartagena.transcaribe.pqrs.domain.exception;

/** El recurso solicitado no existe o el solicitante no tiene por qué verlo. */
public class RecursoNoEncontradoException extends ReglaNegocioException {

    public RecursoNoEncontradoException(String recurso, Object identificador) {
        super("RECURSO_NO_ENCONTRADO",
              "No existe %s con identificador %s".formatted(recurso, identificador));
    }
}
