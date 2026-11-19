package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.EstadoIncidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Evento operativo ocurrido sobre un vehículo de la flota.
 *
 * <p>Una incidencia puede nacer de un reporte interno del conductor o quedar
 * asociada a la PQRS de un ciudadano que la presenció. Ese vínculo opcional es
 * lo que permite cruzar la queja con el hecho operativo y medir si el sistema
 * responde a causas reales o solo a percepciones.</p>
 */
@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "consecutivo", nullable = false, length = 20, updatable = false)
    private String consecutivo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_incidencia_id", nullable = false)
    private TipoIncidencia tipoIncidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pqrs_id")
    private Pqrs pqrs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_reporta_id")
    private Usuario usuarioReporta;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoIncidencia estado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected Incidencia() {
        // Requerido por JPA.
    }

    public Incidencia(String consecutivo,
                      Vehiculo vehiculo,
                      Ruta ruta,
                      TipoIncidencia tipoIncidencia,
                      Usuario usuarioReporta,
                      String descripcion,
                      LocalDateTime fechaHora) {
        this.consecutivo = Objects.requireNonNull(consecutivo, "consecutivo");
        this.vehiculo = Objects.requireNonNull(vehiculo, "vehiculo");
        this.ruta = ruta;
        this.tipoIncidencia = Objects.requireNonNull(tipoIncidencia, "tipoIncidencia");
        this.usuarioReporta = usuarioReporta;
        this.descripcion = Objects.requireNonNull(descripcion, "descripcion");
        this.fechaHora = Objects.requireNonNull(fechaHora, "fechaHora");
        this.estado = EstadoIncidencia.REPORTADA;
    }

    public void vincularConSolicitud(Pqrs pqrs) {
        if (this.estado.esFinal()) {
            throw new OperacionNoPermitidaException(
                    "La incidencia %s ya está cerrada".formatted(consecutivo));
        }
        this.pqrs = pqrs;
    }

    public void tomarEnAtencion() {
        if (this.estado != EstadoIncidencia.REPORTADA) {
            throw new OperacionNoPermitidaException(
                    "Solo una incidencia reportada puede pasar a atención");
        }
        this.estado = EstadoIncidencia.EN_ATENCION;
    }

    public void cerrar() {
        if (this.estado != EstadoIncidencia.EN_ATENCION) {
            throw new OperacionNoPermitidaException(
                    "Solo una incidencia en atención puede cerrarse");
        }
        this.estado = EstadoIncidencia.CERRADA;
    }

    public Prioridad prioridad() {
        return tipoIncidencia.getPrioridadBase();
    }

    public Long getId() {
        return id;
    }

    public String getConsecutivo() {
        return consecutivo;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public Ruta getRuta() {
        return ruta;
    }

    public TipoIncidencia getTipoIncidencia() {
        return tipoIncidencia;
    }

    public Pqrs getPqrs() {
        return pqrs;
    }

    public Usuario getUsuarioReporta() {
        return usuarioReporta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
