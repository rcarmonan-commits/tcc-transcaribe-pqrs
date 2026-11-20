package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.PqrsJpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptador que satisface el puerto de persistencia de PQRS usando JPA.
 *
 * <p>Es la pieza que mantiene la dependencia apuntando hacia el dominio: el
 * núcleo declara qué necesita y esta clase, en el borde del sistema, lo
 * resuelve con la tecnología del momento. Si mañana el almacén fuera otro,
 * esta clase se sustituye y ni una regla de negocio se entera.</p>
 */
@Repository
public class PqrsRepositoryAdapter implements PqrsRepositoryPort {

    private final PqrsJpaRepository repositorio;
    private final Reloj reloj;

    public PqrsRepositoryAdapter(PqrsJpaRepository repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public Pqrs guardar(Pqrs pqrs) {
        return repositorio.save(pqrs);
    }

    @Override
    public Optional<Pqrs> buscarPorRadicado(String radicado) {
        return repositorio.findByRadicado(radicado);
    }

    @Override
    public Optional<Pqrs> buscarPorId(Long id) {
        return repositorio.findById(id);
    }

    @Override
    public boolean existeRadicado(String radicado) {
        return repositorio.existsByRadicado(radicado);
    }

    @Override
    public Pagina<Pqrs> buscar(FiltroPqrs filtro, Paginacion paginacion) {
        return TraductorPaginacion.aPagina(
                repositorio.findAll(
                        EspecificacionesPqrs.desde(filtro, reloj.hoy()),
                        TraductorPaginacion.aPageablePqrs(paginacion)));
    }

    @Override
    public List<Pqrs> buscarVencidasSinRespuesta(LocalDate fechaReferencia) {
        return repositorio.buscarVencidasSinRespuesta(fechaReferencia);
    }

    @Override
    public long contarPorEstado(String codigoEstado) {
        return repositorio.countByEstadoCodigo(codigoEstado);
    }

    @Override
    public long contarRadicadasEntre(LocalDate desde, LocalDate hasta) {
        return repositorio.countByFechaRadicacionBetween(
                desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));
    }

    @Override
    public long contarResueltasDentroDePlazo() {
        return repositorio.countByRespondidaEnPlazo(true);
    }

    @Override
    public long contarResueltasFueraDePlazo() {
        return repositorio.countByRespondidaEnPlazo(false);
    }
}
