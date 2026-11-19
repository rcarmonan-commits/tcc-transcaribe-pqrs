package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.EstadoOperativo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

/** Bus vinculado a la flota del sistema. */
@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "numero_interno", nullable = false, length = 10)
    private String numeroInterno;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_vehiculo_id", nullable = false)
    private TipoVehiculo tipoVehiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 20)
    private EstadoOperativo estadoOperativo;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDate fechaVinculacion;

    protected Vehiculo() {
        // Requerido por JPA.
    }

    public Vehiculo(String placa,
                    String numeroInterno,
                    TipoVehiculo tipoVehiculo,
                    LocalDate fechaVinculacion) {
        this.placa = Objects.requireNonNull(placa, "placa").toUpperCase();
        this.numeroInterno = Objects.requireNonNull(numeroInterno, "numeroInterno");
        this.tipoVehiculo = Objects.requireNonNull(tipoVehiculo, "tipoVehiculo");
        this.fechaVinculacion = Objects.requireNonNull(fechaVinculacion, "fechaVinculacion");
        this.estadoOperativo = EstadoOperativo.OPERATIVO;
    }

    public void cambiarEstadoOperativo(EstadoOperativo nuevo) {
        this.estadoOperativo = Objects.requireNonNull(nuevo, "estadoOperativo");
    }

    public boolean prestaServicio() {
        return estadoOperativo.prestaServicio();
    }

    public int capacidadNominal() {
        return tipoVehiculo.getCapacidadNominal();
    }

    public Long getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public String getNumeroInterno() {
        return numeroInterno;
    }

    public TipoVehiculo getTipoVehiculo() {
        return tipoVehiculo;
    }

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public LocalDate getFechaVinculacion() {
        return fechaVinculacion;
    }
}
