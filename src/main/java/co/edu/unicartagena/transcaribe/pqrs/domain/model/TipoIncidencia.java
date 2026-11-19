package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Clasificación de una incidencia de flota y su prioridad mínima de atención. */
@Entity
@Immutable
@Table(name = "tipo_incidencia")
public class TipoIncidencia {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad_base", nullable = false, length = 10)
    private Prioridad prioridadBase;

    protected TipoIncidencia() {
        // Requerido por JPA.
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Prioridad getPrioridadBase() {
        return prioridadBase;
    }
}
