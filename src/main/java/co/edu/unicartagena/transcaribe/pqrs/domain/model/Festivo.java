package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Día festivo nacional. Se persiste en lugar de calcularse en código para que
 * un ajuste del calendario no obligue a redesplegar el servicio.
 */
@Entity
@Table(name = "festivo")
public class Festivo {

    @Id
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "descripcion", nullable = false, length = 80)
    private String descripcion;

    protected Festivo() {
        // Requerido por JPA.
    }

    public Festivo(LocalDate fecha, String descripcion) {
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
