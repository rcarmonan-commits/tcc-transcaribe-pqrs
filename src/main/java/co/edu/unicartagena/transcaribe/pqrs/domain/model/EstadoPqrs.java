package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Catálogo de estados de una PQRS.
 *
 * <p>La fila aporta la presentación (nombre legible, orden en el tablero); la
 * regla de transición la aporta {@link CodigoEstadoPqrs}. Si alguien inserta
 * un estado desconocido en la tabla, {@link #codigoEstado()} falla de inmediato
 * en lugar de dejar circular un valor que ninguna regla sabe gobernar.</p>
 */
@Entity
@Immutable
@Table(name = "estado_pqrs")
public class EstadoPqrs {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "es_final", nullable = false)
    private boolean esFinal;

    @Column(name = "orden", nullable = false)
    private int orden;

    protected EstadoPqrs() {
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

    public boolean isEsFinal() {
        return esFinal;
    }

    public int getOrden() {
        return orden;
    }

    /** Traduce la fila del catálogo al tipo que gobierna las transiciones. */
    public CodigoEstadoPqrs codigoEstado() {
        return CodigoEstadoPqrs.valueOf(codigo);
    }

    public boolean es(CodigoEstadoPqrs esperado) {
        return codigoEstado() == esperado;
    }
}
