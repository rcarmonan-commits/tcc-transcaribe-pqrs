package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CiudadanoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.IncidenciaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.RutaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.VehiculoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.CiudadanoJpaRepository;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.IncidenciaJpaRepository;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.RutaJpaRepository;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.UsuarioJpaRepository;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.VehiculoJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptadores de persistencia de las entidades de operación.
 *
 * <p>Se agrupan como clases anidadas en un mismo archivo porque cada una es un
 * reenvío literal de cinco líneas al repositorio de Spring Data. Repartirlas en
 * cinco archivos no añadiría cohesión: añadiría cinco sitios donde buscar lo
 * mismo. El adaptador de PQRS, en cambio, sí vive aparte, porque tiene lógica
 * propia de traducción de filtros y paginación.</p>
 */
public final class AdaptadoresOperacion {

    private AdaptadoresOperacion() {
        // Contenedor de adaptadores.
    }

    @Repository
    public static class CiudadanoAdapter implements CiudadanoRepositoryPort {

        private final CiudadanoJpaRepository repositorio;

        public CiudadanoAdapter(CiudadanoJpaRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public Ciudadano guardar(Ciudadano ciudadano) {
            return repositorio.save(ciudadano);
        }

        @Override
        public Optional<Ciudadano> buscarPorDocumento(String codigoTipoDocumento, String numeroDocumento) {
            return repositorio.findByTipoDocumentoCodigoAndNumeroDocumento(codigoTipoDocumento, numeroDocumento);
        }

        @Override
        public Optional<Ciudadano> buscarPorId(Long id) {
            return repositorio.findById(id);
        }

        @Override
        public boolean existeDocumento(String codigoTipoDocumento, String numeroDocumento) {
            return repositorio.existsByTipoDocumentoCodigoAndNumeroDocumento(codigoTipoDocumento, numeroDocumento);
        }
    }

    @Repository
    public static class VehiculoAdapter implements VehiculoRepositoryPort {

        private final VehiculoJpaRepository repositorio;

        public VehiculoAdapter(VehiculoJpaRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public Vehiculo guardar(Vehiculo vehiculo) {
            return repositorio.save(vehiculo);
        }

        @Override
        public Optional<Vehiculo> buscarPorPlaca(String placa) {
            return repositorio.findByPlaca(placa);
        }

        @Override
        public boolean existePlaca(String placa) {
            return repositorio.existsByPlaca(placa);
        }

        @Override
        public boolean existeNumeroInterno(String numeroInterno) {
            return repositorio.existsByNumeroInterno(numeroInterno);
        }

        @Override
        public Pagina<Vehiculo> listar(Paginacion paginacion) {
            return TraductorPaginacion.aPagina(
                    repositorio.findAll(TraductorPaginacion.aPageableFlota(paginacion, "placa")));
        }
    }

    @Repository
    public static class RutaAdapter implements RutaRepositoryPort {

        private final RutaJpaRepository repositorio;

        public RutaAdapter(RutaJpaRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public Ruta guardar(Ruta ruta) {
            return repositorio.save(ruta);
        }

        @Override
        public Optional<Ruta> buscarPorCodigo(String codigo) {
            return repositorio.findByCodigo(codigo);
        }

        @Override
        public boolean existeCodigo(String codigo) {
            return repositorio.existsByCodigo(codigo);
        }

        @Override
        public List<Ruta> listarActivas() {
            return repositorio.findByActivaTrueOrderByCodigoAsc();
        }
    }

    @Repository
    public static class IncidenciaAdapter implements IncidenciaRepositoryPort {

        private final IncidenciaJpaRepository repositorio;

        public IncidenciaAdapter(IncidenciaJpaRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public Incidencia guardar(Incidencia incidencia) {
            return repositorio.save(incidencia);
        }

        @Override
        public Optional<Incidencia> buscarPorConsecutivo(String consecutivo) {
            return repositorio.findByConsecutivo(consecutivo);
        }

        @Override
        public Pagina<Incidencia> listarPorVehiculo(String placa, Paginacion paginacion) {
            return TraductorPaginacion.aPagina(repositorio.findByVehiculoPlaca(
                    placa, TraductorPaginacion.aPageableFlota(paginacion, "fechaHora")));
        }

        @Override
        public Pagina<Incidencia> listar(Paginacion paginacion) {
            return TraductorPaginacion.aPagina(
                    repositorio.findAll(TraductorPaginacion.aPageableFlota(paginacion, "fechaHora")));
        }

        @Override
        public long contarPorTipo(String codigoTipo) {
            return repositorio.countByTipoIncidenciaCodigo(codigoTipo);
        }

        @Override
        public long contarDelAnio(int anio) {
            return repositorio.countByFechaHoraBetween(
                    LocalDateTime.of(anio, 1, 1, 0, 0),
                    LocalDateTime.of(anio, 12, 31, 23, 59, 59));
        }
    }

    @Repository
    public static class UsuarioAdapter implements UsuarioRepositoryPort {

        private final UsuarioJpaRepository repositorio;

        public UsuarioAdapter(UsuarioJpaRepository repositorio) {
            this.repositorio = repositorio;
        }

        @Override
        public Usuario guardar(Usuario usuario) {
            return repositorio.save(usuario);
        }

        @Override
        public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
            return repositorio.findByNombreUsuario(nombreUsuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(Long id) {
            return repositorio.findById(id);
        }
    }
}
