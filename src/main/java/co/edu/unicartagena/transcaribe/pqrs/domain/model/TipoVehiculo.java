package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Tipología del vehículo (articulado, padrón, alimentador).
 *
 * <p>La capacidad nominal es un atributo del tipo, no de cada bus: mantenerla
 * aquí elimina la dependencia transitiva que rompería la tercera forma normal.</p>
 */
@Entity
@Immutable
@Table(name = "tipo_vehiculo")
public class TipoVehiculo {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "capacidad_nominal", nullable = false)
    private int capacidadNominal;

    protected TipoVehiculo() {
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

    public int getCapacidadNominal() {
        return capacidadNominal;
    }
}
