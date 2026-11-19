package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entrada inmutable de la bitácora de una solicitud.
 *
 * <p>No tiene métodos de modificación a propósito: una traza que se puede
 * editar no sirve como evidencia. Es el soporte del control A09:2021 de OWASP
 * (fallas de registro y monitoreo) y de la rendición de cuentas frente a la
 * autoridad de control.</p>
 */
@Entity
@Table(name = "seguimiento_pqrs")
public class SeguimientoPqrs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pqrs_id", nullable = false)
    private Pqrs pqrs;

    /** Nulo únicamente en el registro de creación: antes no había estado. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_anterior_id")
    private EstadoPqrs estadoAnterior;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoPqrs estadoNuevo;

    /** Nulo cuando la transición la produjo el propio sistema, no una persona. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "observacion", nullable = false, length = 1000)
    private String observacion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    protected SeguimientoPqrs() {
        // Requerido por JPA.
    }

    SeguimientoPqrs(Pqrs pqrs,
                    EstadoPqrs estadoAnterior,
                    EstadoPqrs estadoNuevo,
                    Usuario usuario,
                    String observacion,
                    LocalDateTime fechaRegistro) {
        this.pqrs = pqrs;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.observacion = (observacion == null || observacion.isBlank())
                ? "Sin observación"
                : observacion;
        this.fechaRegistro = fechaRegistro;
    }

    /** Primera entrada de la bitácora, creada junto con la solicitud. */
    static SeguimientoPqrs deCreacion(Pqrs pqrs, EstadoPqrs estadoInicial, LocalDateTime fecha) {
        return new SeguimientoPqrs(pqrs, null, estadoInicial, null,
                "Solicitud radicada por el canal de atención", fecha);
    }

    public Long getId() {
        return id;
    }

    public Pqrs getPqrs() {
        return pqrs;
    }

    public EstadoPqrs getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoPqrs getEstadoNuevo() {
        return estadoNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
