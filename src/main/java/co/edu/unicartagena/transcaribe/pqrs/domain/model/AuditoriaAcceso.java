package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registro de un evento de seguridad (OWASP A09:2021).
 *
 * <p>La dirección IP se guarda como resumen criptográfico: permite correlacionar
 * una ráfaga de intentos desde un mismo origen sin almacenar un dato personal
 * en claro, que es lo que exige el principio de minimización de la Ley 1581 de
 * 2012 sobre protección de datos.</p>
 */
@Entity
@Table(name = "auditoria_acceso")
public class AuditoriaAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, length = 60)
    private String nombreUsuario;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "recurso", nullable = false, length = 200)
    private String recurso;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    protected AuditoriaAcceso() {
        // Requerido por JPA.
    }

    public AuditoriaAcceso(String nombreUsuario,
                           String accion,
                           String recurso,
                           String ipHash,
                           boolean exitoso,
                           LocalDateTime fecha) {
        this.nombreUsuario = nombreUsuario;
        this.accion = accion;
        this.recurso = recurso;
        this.ipHash = ipHash;
        this.exitoso = exitoso;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getIpHash() {
        return ipHash;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
