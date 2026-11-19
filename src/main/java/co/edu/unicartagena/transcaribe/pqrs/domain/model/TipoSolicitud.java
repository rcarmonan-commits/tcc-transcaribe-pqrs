package co.edu.unicartagena.transcaribe.pqrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Tipo de solicitud ciudadana y plazo legal asociado.
 *
 * <p>El plazo vive aquí y no en cada PQRS: eso es lo que mantiene el modelo en
 * tercera forma normal y, sobre todo, lo que permite que un cambio normativo
 * se aplique en un solo lugar (RN-01, Ley 1755 de 2015, art. 14).</p>
 */
@Entity
@Immutable
@Table(name = "tipo_solicitud")
public class TipoSolicitud {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "dias_habiles_respuesta", nullable = false)
    private int diasHabilesRespuesta;

    @Column(name = "fundamento_legal", nullable = false, length = 160)
    private String fundamentoLegal;

    protected TipoSolicitud() {
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

    public int getDiasHabilesRespuesta() {
        return diasHabilesRespuesta;
    }

    public String getFundamentoLegal() {
        return fundamentoLegal;
    }
}
