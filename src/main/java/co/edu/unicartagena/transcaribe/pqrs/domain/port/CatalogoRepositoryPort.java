package co.edu.unicartagena.transcaribe.pqrs.domain.port;

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
import java.util.List;
import java.util.Optional;

/**
 * Acceso de solo lectura a los catálogos del sistema.
 *
 * <p>Se agrupan en un único puerto porque comparten el mismo motivo de cambio
 * —son datos de referencia inmutables— y separarlos en ocho interfaces de un
 * método cada una multiplicaría la ceremonia sin aportar sustituibilidad real.
 * La segregación de interfaces pide no obligar a un cliente a depender de lo
 * que no usa; aquí todos los clientes del catálogo usan varias de estas
 * consultas a la vez.</p>
 */
public interface CatalogoRepositoryPort {

    Optional<TipoDocumento> tipoDocumentoPorCodigo(String codigo);

    Optional<TipoSolicitud> tipoSolicitudPorCodigo(String codigo);

    Optional<CategoriaPqrs> categoriaPorCodigo(String codigo);

    Optional<CanalRecepcion> canalPorCodigo(String codigo);

    Optional<EstadoPqrs> estadoPorCodigo(CodigoEstadoPqrs codigo);

    Optional<TipoVehiculo> tipoVehiculoPorCodigo(String codigo);

    Optional<TipoIncidencia> tipoIncidenciaPorCodigo(String codigo);

    Optional<Rol> rolPorCodigo(String codigo);

    List<Festivo> listarFestivos();

    List<TipoSolicitud> listarTiposSolicitud();

    List<CategoriaPqrs> listarCategoriasActivas();

    List<CanalRecepcion> listarCanales();
}
