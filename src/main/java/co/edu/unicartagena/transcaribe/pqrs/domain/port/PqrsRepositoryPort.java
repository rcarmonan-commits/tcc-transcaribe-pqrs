package co.edu.unicartagena.transcaribe.pqrs.domain.port;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del agregado PQRS.
 *
 * <p>Lo declara el dominio y lo implementa la infraestructura: la dependencia
 * apunta hacia adentro, no hacia la base de datos.</p>
 */
public interface PqrsRepositoryPort {

    Pqrs guardar(Pqrs pqrs);

    Optional<Pqrs> buscarPorRadicado(String radicado);

    Optional<Pqrs> buscarPorId(Long id);

    boolean existeRadicado(String radicado);

    Pagina<Pqrs> buscar(FiltroPqrs filtro, Paginacion paginacion);

    /** Solicitudes cuya fecha límite ya pasó y siguen sin respuesta. */
    List<Pqrs> buscarVencidasSinRespuesta(LocalDate fechaReferencia);

    long contarPorEstado(String codigoEstado);

    long contarRadicadasEntre(LocalDate desde, LocalDate hasta);

    long contarResueltasDentroDePlazo();

    long contarResueltasFueraDePlazo();
}
