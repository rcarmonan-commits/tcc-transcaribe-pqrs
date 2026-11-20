package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.CanalRecepcion;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.CategoriaPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.EstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Festivo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Rol;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoDocumento;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoIncidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoSolicitud;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoVehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceso a los catálogos mediante una única consulta genérica por código.
 *
 * <p>Son nueve tablas con la misma forma —identificador, código, nombre— y la
 * misma consulta. Escribir nueve interfaces de repositorio idénticas sería
 * repetir la misma línea nueve veces; una consulta parametrizada por tipo las
 * resuelve todas.</p>
 *
 * <p>Sobre la concatenación de {@code tipo.getSimpleName()} en el JPQL: el
 * valor procede de un literal de clase resuelto en compilación, jamás de una
 * entrada del usuario, de modo que no abre ninguna vía de inyección. El
 * <em>código</em> buscado, que sí viene de fuera, viaja siempre como parámetro
 * vinculado.</p>
 */
@Repository
public class CatalogoRepositoryAdapter implements CatalogoRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoDocumento> tipoDocumentoPorCodigo(String codigo) {
        return porCodigo(TipoDocumento.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoSolicitud> tipoSolicitudPorCodigo(String codigo) {
        return porCodigo(TipoSolicitud.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoriaPqrs> categoriaPorCodigo(String codigo) {
        return porCodigo(CategoriaPqrs.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanalRecepcion> canalPorCodigo(String codigo) {
        return porCodigo(CanalRecepcion.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadoPqrs> estadoPorCodigo(CodigoEstadoPqrs codigo) {
        return codigo == null ? Optional.empty() : porCodigo(EstadoPqrs.class, codigo.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoVehiculo> tipoVehiculoPorCodigo(String codigo) {
        return porCodigo(TipoVehiculo.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoIncidencia> tipoIncidenciaPorCodigo(String codigo) {
        return porCodigo(TipoIncidencia.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> rolPorCodigo(String codigo) {
        return porCodigo(Rol.class, codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Festivo> listarFestivos() {
        return entityManager
                .createQuery("SELECT f FROM Festivo f ORDER BY f.fecha ASC", Festivo.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoSolicitud> listarTiposSolicitud() {
        return todos(TipoSolicitud.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaPqrs> listarCategoriasActivas() {
        return entityManager
                .createQuery("SELECT c FROM CategoriaPqrs c WHERE c.activa = true ORDER BY c.nombre ASC",
                        CategoriaPqrs.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalRecepcion> listarCanales() {
        return todos(CanalRecepcion.class);
    }

    private <T> Optional<T> porCodigo(Class<T> tipo, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return entityManager
                .createQuery("SELECT e FROM " + tipo.getSimpleName() + " e WHERE e.codigo = :codigo", tipo)
                .setParameter("codigo", codigo)
                .getResultStream()
                .findFirst();
    }

    private <T> List<T> todos(Class<T> tipo) {
        return entityManager
                .createQuery("SELECT e FROM " + tipo.getSimpleName() + " e ORDER BY e.nombre ASC", tipo)
                .getResultList();
    }
}
