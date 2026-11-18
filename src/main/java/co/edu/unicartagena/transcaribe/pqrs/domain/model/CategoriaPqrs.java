package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Categoría temática de la solicitud (seguridad, tarifa, accesibilidad…).
 *
 * <p>Cada categoría declara la prioridad mínima con la que debe atenderse una
 * PQRS que cae en ella. Es el dato que alimenta la política de priorización
 * sin que esta tenga que conocer la lista de categorías (RN-06).</p>
 */
@Entity
@Immutable
@Table(name = "categoria_pqrs")
public class CategoriaPqrs {

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

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected CategoriaPqrs() {
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

    public boolean isActiva() {
        return activa;
    }
}
