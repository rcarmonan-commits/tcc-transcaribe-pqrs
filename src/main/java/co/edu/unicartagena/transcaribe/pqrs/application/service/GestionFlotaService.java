package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearRutaPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearVehiculoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.RutaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.VehiculoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.FlotaMapper;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoDuplicadoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoVehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.RutaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.VehiculoRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de la flota y de las rutas del sistema (CU-07, CU-08).
 *
 * <p>Las comprobaciones de unicidad se hacen antes de intentar escribir,
 * aunque la base de datos ya tenga la restricción. No es redundancia inútil:
 * permite devolver un error de negocio claro en lugar de dejar escapar una
 * violación de restricción del motor, que además revelaría el nombre del
 * índice al cliente.</p>
 */
@Service
public class GestionFlotaService {

    private final VehiculoRepositoryPort vehiculoRepositorio;
    private final RutaRepositoryPort rutaRepositorio;
    private final CatalogoRepositoryPort catalogo;
    private final SanitizadorTexto sanitizador;
    private final FlotaMapper mapper;

    public GestionFlotaService(VehiculoRepositoryPort vehiculoRepositorio,
                               RutaRepositoryPort rutaRepositorio,
                               CatalogoRepositoryPort catalogo,
                               SanitizadorTexto sanitizador,
                               FlotaMapper mapper) {
        this.vehiculoRepositorio = vehiculoRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.catalogo = catalogo;
        this.sanitizador = sanitizador;
        this.mapper = mapper;
    }

    @Transactional
    public VehiculoRespuesta registrarVehiculo(CrearVehiculoPeticion peticion) {
        String placa = peticion.placa().toUpperCase();

        if (vehiculoRepositorio.existePlaca(placa)) {
            throw new RecursoDuplicadoException("vehículo", placa);
        }
        if (vehiculoRepositorio.existeNumeroInterno(peticion.numeroInterno())) {
            throw new RecursoDuplicadoException("vehículo con número interno", peticion.numeroInterno());
        }

        TipoVehiculo tipo = catalogo.tipoVehiculoPorCodigo(peticion.tipoVehiculo())
                .orElseThrow(() -> new RecursoNoEncontradoException("tipo de vehículo", peticion.tipoVehiculo()));

        Vehiculo vehiculo = new Vehiculo(placa, peticion.numeroInterno(), tipo, peticion.fechaVinculacion());
        return mapper.aVehiculo(vehiculoRepositorio.guardar(vehiculo));
    }

    @Transactional(readOnly = true)
    public VehiculoRespuesta consultarVehiculo(String placa) {
        return vehiculoRepositorio.buscarPorPlaca(placa.toUpperCase())
                .map(mapper::aVehiculo)
                .orElseThrow(() -> new RecursoNoEncontradoException("vehículo", placa));
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<VehiculoRespuesta> listarVehiculos(Paginacion paginacion) {
        return PaginaRespuesta.desde(
                vehiculoRepositorio.listar(paginacion).mapear(mapper::aVehiculo));
    }

    @Transactional
    public RutaRespuesta registrarRuta(CrearRutaPeticion peticion) {
        String codigo = peticion.codigo().toUpperCase();

        if (rutaRepositorio.existeCodigo(codigo)) {
            throw new RecursoDuplicadoException("ruta", codigo);
        }

        Ruta ruta = new Ruta(
                codigo,
                sanitizador.limpiar(peticion.nombre()),
                sanitizador.limpiar(peticion.origen()),
                sanitizador.limpiar(peticion.destino()));

        return mapper.aRuta(rutaRepositorio.guardar(ruta));
    }

    @Transactional(readOnly = true)
    public List<RutaRespuesta> listarRutasActivas() {
        return rutaRepositorio.listarActivas().stream()
                .map(mapper::aRuta)
                .toList();
    }
}
