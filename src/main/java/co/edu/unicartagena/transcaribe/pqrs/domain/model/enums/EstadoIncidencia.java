package co.edu.unicartagena.transcaribe.pqrs.domain.model.enums;

/** Ciclo de vida de una incidencia de flota. */
public enum EstadoIncidencia {

    REPORTADA,
    EN_ATENCION,
    CERRADA;

    public boolean esFinal() {
        return this == CERRADA;
    }
}
