package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.TransicionInvalidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Solicitud ciudadana: petición, queja, reclamo, sugerencia o denuncia.
 *
 * <p>Es la raíz del agregado. Ninguna capa externa cambia su estado por
 * asignación directa: toda modificación pasa por un método que valida la regla
 * y deja la traza correspondiente en {@link SeguimientoPqrs}. De ahí que no
 * existan setters públicos — es la única forma de garantizar que la bitácora
 * y el estado nunca se desincronicen (RN-09).</p>
 *
 * <p>Reglas que custodia este agregado:</p>
 * <ul>
 *   <li><b>RN-03</b> una solicitud cerrada no admite más cambios.</li>
 *   <li><b>RN-04</b> solo se permiten las transiciones del grafo de estados.</li>
 *   <li><b>RN-09</b> toda transición queda registrada con autor y observación.</li>
 *   <li><b>RN-10</b> el cierre exige una justificación de al menos 20 caracteres.</li>
 * </ul>
 */
@Entity
@Table(name = "pqrs")
public class Pqrs {

    /** Longitud mínima de la justificación de cierre (RN-10). */
    public static final int LONGITUD_MINIMA_JUSTIFICACION = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "radicado", nullable = false, length = 20, updatable = false)
    private String radicado;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_solicitud_id", nullable = false)
    private TipoSolicitud tipoSolicitud;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaPqrs categoria;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "canal_id", nullable = false)
    private CanalRecepcion canal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoPqrs estado;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciudadano_id", nullable = false)
    private Ciudadano ciudadano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asignado_id")
    private Usuario usuarioAsignado;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private Prioridad prioridad;

    @Column(name = "asunto", nullable = false, length = 180)
    private String asunto;

    @Column(name = "descripcion", nullable = false, length = 4000)
    private String descripcion;

    @Column(name = "respuesta", length = 4000)
    private String respuesta;

    @Column(name = "fecha_radicacion", nullable = false, updatable = false)
    private LocalDateTime fechaRadicacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    /**
     * Si la respuesta se dio dentro del término legal. Se fija una sola vez, al
     * responder, y queda congelada: el cumplimiento se juzga con el calendario
     * vigente ese día, no con el de la fecha en que alguien consulte el informe.
     */
    @Column(name = "respondida_en_plazo")
    private Boolean respondidaEnPlazo;

    @OneToMany(mappedBy = "pqrs", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fechaRegistro ASC")
    private List<SeguimientoPqrs> seguimientos = new ArrayList<>();

    protected Pqrs() {
        // Requerido por JPA.
    }

    /**
     * Crea una solicitud ya radicada y deja el primer registro de la bitácora.
     * Los valores derivados —radicado, prioridad y fecha de vencimiento— llegan
     * calculados desde los servicios de dominio correspondientes: el agregado
     * los custodia, no los calcula.
     */
    public Pqrs(String radicado,
                TipoSolicitud tipoSolicitud,
                CategoriaPqrs categoria,
                CanalRecepcion canal,
                EstadoPqrs estadoInicial,
                Ciudadano ciudadano,
                Prioridad prioridad,
                String asunto,
                String descripcion,
                LocalDateTime fechaRadicacion,
                LocalDate fechaVencimiento) {

        if (!estadoInicial.es(CodigoEstadoPqrs.RADICADA)) {
            throw new OperacionNoPermitidaException(
                    "Una solicitud nueva solo puede nacer en estado RADICADA");
        }
        this.radicado = Objects.requireNonNull(radicado, "radicado");
        this.tipoSolicitud = Objects.requireNonNull(tipoSolicitud, "tipoSolicitud");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.estado = estadoInicial;
        this.ciudadano = Objects.requireNonNull(ciudadano, "ciudadano");
        this.prioridad = Objects.requireNonNull(prioridad, "prioridad");
        this.asunto = Objects.requireNonNull(asunto, "asunto");
        this.descripcion = Objects.requireNonNull(descripcion, "descripcion");
        this.fechaRadicacion = Objects.requireNonNull(fechaRadicacion, "fechaRadicacion");
        this.fechaVencimiento = Objects.requireNonNull(fechaVencimiento, "fechaVencimiento");

        this.seguimientos.add(SeguimientoPqrs.deCreacion(this, estadoInicial, fechaRadicacion));
    }

    /** Asocia la solicitud al servicio concreto sobre el que reclama el ciudadano. */
    public void asociarServicio(Ruta ruta, Vehiculo vehiculo) {
        this.ruta = ruta;
        this.vehiculo = vehiculo;
    }

    public void asignarA(Usuario funcionario) {
        verificarQueNoEsteCerrada();
        this.usuarioAsignado = funcionario;
    }

    /**
     * Mueve la solicitud a un nuevo estado y deja la traza correspondiente.
     *
     * @throws TransicionInvalidaException   si el grafo no permite el paso (RN-04)
     * @throws OperacionNoPermitidaException si está cerrada o falta justificar el cierre
     */
    public SeguimientoPqrs cambiarEstado(EstadoPqrs nuevoEstado,
                                         String observacion,
                                         Usuario autor,
                                         LocalDateTime ahora) {
        Objects.requireNonNull(nuevoEstado, "nuevoEstado");
        verificarQueNoEsteCerrada();

        CodigoEstadoPqrs origen = this.estado.codigoEstado();
        CodigoEstadoPqrs destino = nuevoEstado.codigoEstado();

        if (!origen.permiteTransicionA(destino)) {
            throw new TransicionInvalidaException(origen, destino);
        }
        if (destino == CodigoEstadoPqrs.CERRADA) {
            exigirJustificacion(observacion);
        }

        EstadoPqrs anterior = this.estado;
        this.estado = nuevoEstado;

        SeguimientoPqrs traza = new SeguimientoPqrs(this, anterior, nuevoEstado, autor, observacion, ahora);
        this.seguimientos.add(traza);
        return traza;
    }

    /**
     * Registra la respuesta de fondo y deja la solicitud en estado RESUELTA.
     * Solo tiene sentido sobre una solicitud en trámite o vencida.
     */
    public SeguimientoPqrs responder(String textoRespuesta,
                                     EstadoPqrs estadoResuelta,
                                     Usuario autor,
                                     LocalDateTime ahora) {
        verificarQueNoEsteCerrada();
        if (!this.estado.codigoEstado().admiteRespuesta()) {
            throw new OperacionNoPermitidaException(
                    "Una solicitud en estado %s no admite respuesta de fondo"
                            .formatted(this.estado.getCodigo()));
        }
        if (textoRespuesta == null || textoRespuesta.isBlank()) {
            throw new OperacionNoPermitidaException("La respuesta no puede estar vacía");
        }

        this.respuesta = textoRespuesta;
        this.fechaRespuesta = ahora;
        this.respondidaEnPlazo = !ahora.toLocalDate().isAfter(fechaVencimiento);
        return cambiarEstado(estadoResuelta, "Respuesta de fondo registrada", autor, ahora);
    }

    /**
     * Una solicitud está vencida cuando pasó su fecha límite sin respuesta.
     * La comparación se hace contra la fecha recibida y no contra el reloj del
     * sistema: así la regla es verificable en una prueba unitaria.
     */
    public boolean estaVencida(LocalDate fechaReferencia) {
        if (fechaRespuesta != null || this.estado.isEsFinal()) {
            return false;
        }
        return fechaReferencia.isAfter(fechaVencimiento);
    }

    /** Días de calendario que restan hasta el vencimiento; negativo si ya se incumplió. */
    public long diasRestantes(LocalDate fechaReferencia) {
        return ChronoUnit.DAYS.between(fechaReferencia, fechaVencimiento);
    }

    private void verificarQueNoEsteCerrada() {
        if (this.estado.isEsFinal()) {
            throw new OperacionNoPermitidaException(
                    "La solicitud %s está cerrada y no admite cambios".formatted(radicado));
        }
    }

    private void exigirJustificacion(String observacion) {
        if (observacion == null || observacion.strip().length() < LONGITUD_MINIMA_JUSTIFICACION) {
            throw new OperacionNoPermitidaException(
                    "El cierre exige una justificación de al menos %d caracteres"
                            .formatted(LONGITUD_MINIMA_JUSTIFICACION));
        }
    }

    public Long getId() {
        return id;
    }

    public String getRadicado() {
        return radicado;
    }

    public TipoSolicitud getTipoSolicitud() {
        return tipoSolicitud;
    }

    public CategoriaPqrs getCategoria() {
        return categoria;
    }

    public CanalRecepcion getCanal() {
        return canal;
    }

    public EstadoPqrs getEstado() {
        return estado;
    }

    public Ciudadano getCiudadano() {
        return ciudadano;
    }

    public Ruta getRuta() {
        return ruta;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public Usuario getUsuarioAsignado() {
        return usuarioAsignado;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public LocalDateTime getFechaRadicacion() {
        return fechaRadicacion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    /** {@code null} mientras la solicitud no tenga respuesta de fondo. */
    public Boolean getRespondidaEnPlazo() {
        return respondidaEnPlazo;
    }

    public List<SeguimientoPqrs> getSeguimientos() {
        return Collections.unmodifiableList(seguimientos);
    }
}
