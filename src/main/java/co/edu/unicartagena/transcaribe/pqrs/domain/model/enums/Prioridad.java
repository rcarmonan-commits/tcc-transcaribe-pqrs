package co.edu.unicartagena.transcaribe.pqrs.domain.model.enums;

/**
 * Prioridad de atención de una solicitud o incidencia.
 *
 * <p>El orden declarado es significativo: {@link #compareTo} refleja la
 * severidad, de modo que la política de priorización puede quedarse con el
 * máximo sin conocer los valores concretos (principio abierto/cerrado).</p>
 */
public enum Prioridad {

    BAJA,
    MEDIA,
    ALTA,
    CRITICA;

    /** @return el más severo de los dos niveles. */
    public Prioridad maximo(Prioridad otra) {
        return this.compareTo(otra) >= 0 ? this : otra;
    }

    public boolean esAlMenos(Prioridad umbral) {
        return this.compareTo(umbral) >= 0;
    }
}
