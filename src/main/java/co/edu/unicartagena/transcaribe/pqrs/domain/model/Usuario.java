package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Funcionario de Transcaribe que opera el sistema.
 *
 * <p>Concentra aquí la política de bloqueo por intentos fallidos porque es una
 * regla de negocio, no de infraestructura: el servicio de autenticación
 * pregunta al agregado si puede entrar y le informa el resultado, sin
 * manipular contadores por su cuenta (encapsulamiento y responsabilidad única).
 * Mitiga OWASP A07:2021 — Fallas de identificación y autenticación.</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, length = 60)
    private String nombreUsuario;

    @Column(name = "correo", nullable = false, length = 160)
    private String correo;

    /** Resumen BCrypt. La contraseña en claro no existe en ningún punto del modelo. */
    @Column(name = "hash_contrasena", nullable = false, length = 100)
    private String hashContrasena;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() {
        // Requerido por JPA.
    }

    public Usuario(String nombreUsuario,
                   String correo,
                   String hashContrasena,
                   String nombres,
                   String apellidos,
                   LocalDateTime fechaCreacion) {
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaCreacion = fechaCreacion;
        this.activo = true;
        this.intentosFallidos = 0;
    }

    /** @return {@code true} si la cuenta está bloqueada en el instante indicado. */
    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean puedeAutenticarse(LocalDateTime ahora) {
        return activo && !estaBloqueado(ahora);
    }

    /**
     * Registra un intento fallido y bloquea la cuenta al alcanzar el umbral.
     *
     * @param intentosMaximos número de fallos tolerados antes del bloqueo
     * @param minutosBloqueo  duración del bloqueo
     * @param ahora           instante de referencia, inyectado para poder probarlo
     */
    public void registrarIntentoFallido(int intentosMaximos, int minutosBloqueo, LocalDateTime ahora) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= intentosMaximos) {
            this.bloqueadoHasta = ahora.plusMinutes(minutosBloqueo);
            this.intentosFallidos = 0;
        }
    }

    /** Una autenticación correcta limpia el contador y levanta el bloqueo vencido. */
    public void registrarAutenticacionExitosa() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public void asignarRol(Rol rol) {
        this.roles.add(rol);
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Set<Rol> getRoles() {
        return Collections.unmodifiableSet(roles);
    }
}
