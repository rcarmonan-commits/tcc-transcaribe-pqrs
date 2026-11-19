package co.edu.unicartagena.transcaribe.pqrs.domain.model.enums;

/** Situación operativa de un vehículo de la flota. */
public enum EstadoOperativo {

    OPERATIVO,
    EN_MANTENIMIENTO,
    FUERA_DE_SERVICIO;

    /** Solo un vehículo operativo puede estar prestando servicio en una ruta. */
    public boolean prestaServicio() {
        return this == OPERATIVO;
    }
}
