package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/** Ruta del sistema integrado (troncal, pretroncal o alimentadora). */
@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "origen", nullable = false, length = 120)
    private String origen;

    @Column(name = "destino", nullable = false, length = 120)
    private String destino;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected Ruta() {
        // Requerido por JPA.
    }

    public Ruta(String codigo, String nombre, String origen, String destino) {
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.origen = Objects.requireNonNull(origen, "origen");
        this.destino = Objects.requireNonNull(destino, "destino");
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
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

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public boolean isActiva() {
        return activa;
    }
}
