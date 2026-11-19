package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Contador anual del número de radicado.
 *
 * <p>Se materializa como fila para poder tomar un bloqueo sobre ella. Una
 * secuencia del motor también serviría, pero ataría el número de radicado al
 * proveedor de base de datos y rompería la portabilidad que exige el
 * requisito no funcional RNF-07.</p>
 */
@Entity
@Table(name = "consecutivo_radicado")
public class ConsecutivoRadicado {

    @Id
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "ultimo_valor", nullable = false)
    private long ultimoValor;

    protected ConsecutivoRadicado() {
        // Requerido por JPA.
    }

    public ConsecutivoRadicado(Integer anio) {
        this.anio = anio;
        this.ultimoValor = 0L;
    }

    /** Avanza el contador y devuelve el valor recién asignado. */
    public long siguiente() {
        return ++this.ultimoValor;
    }

    public Integer getAnio() {
        return anio;
    }

    public long getUltimoValor() {
        return ultimoValor;
    }
}
