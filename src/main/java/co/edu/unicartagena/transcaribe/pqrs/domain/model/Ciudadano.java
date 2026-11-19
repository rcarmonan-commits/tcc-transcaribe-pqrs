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
import java.util.Objects;

/**
 * Usuario del sistema de transporte que radica una solicitud.
 *
 * <p>Es el titular de datos personales del modelo. Por eso expone
 * {@link #documentoEnmascarado()} y {@link #correoEnmascarado()}: la trazas y
 * las respuestas de listado nunca necesitan el dato completo, y publicarlo sin
 * necesidad sería una exposición innecesaria (OWASP A01:2021).</p>
 */
@Entity
@Table(name = "ciudadano")
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo", nullable = false, length = 160)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected Ciudadano() {
        // Requerido por JPA.
    }

    public Ciudadano(TipoDocumento tipoDocumento,
                     String numeroDocumento,
                     String nombres,
                     String apellidos,
                     String correo,
                     String telefono,
                     LocalDateTime fechaRegistro) {
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "tipoDocumento");
        this.numeroDocumento = Objects.requireNonNull(numeroDocumento, "numeroDocumento");
        this.nombres = Objects.requireNonNull(nombres, "nombres");
        this.apellidos = Objects.requireNonNull(apellidos, "apellidos");
        this.correo = Objects.requireNonNull(correo, "correo");
        this.telefono = telefono;
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }

    public void actualizarContacto(String correo, String telefono) {
        this.correo = Objects.requireNonNull(correo, "correo");
        this.telefono = telefono;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    /**
     * Deja visibles únicamente los cuatro últimos dígitos del documento.
     * Suficiente para que un funcionario confirme identidad, insuficiente
     * para reconstruir el dato si la traza se filtra.
     */
    public String documentoEnmascarado() {
        int visibles = 4;
        if (numeroDocumento.length() <= visibles) {
            return "*".repeat(numeroDocumento.length());
        }
        String cola = numeroDocumento.substring(numeroDocumento.length() - visibles);
        return "*".repeat(numeroDocumento.length() - visibles) + cola;
    }

    /** Conserva la primera letra y el dominio: {@code c****@correo.com}. */
    public String correoEnmascarado() {
        int arroba = correo.indexOf('@');
        if (arroba <= 1) {
            return "***" + correo.substring(Math.max(arroba, 0));
        }
        return correo.charAt(0) + "*".repeat(arroba - 1) + correo.substring(arroba);
    }

    public Long getId() {
        return id;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
