package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RegistrarIncidenciaPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.IncidenciaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.FlotaMapper;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoIncidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.IncidenciaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.RutaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.VehiculoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y seguimiento de incidencias de flota (CU-09).
 *
 * <p>El consecutivo de la incidencia se arma con el año y el número de
 * incidencias ya registradas en él. Es suficiente para un identificador
 * operativo interno y, a diferencia del radicado ciudadano, no se expone a
 * terceros ni sirve para recuperar datos personales.</p>
 */
@Service
public class GestionIncidenciasService {

    private static final String PREFIJO_CONSECUTIVO = "INC";

    private final IncidenciaRepositoryPort incidenciaRepositorio;
    private final VehiculoRepositoryPort vehiculoRepositorio;
    private final RutaRepositoryPort rutaRepositorio;
    private final PqrsRepositoryPort pqrsRepositorio;
    private final UsuarioRepositoryPort usuarioRepositorio;
    private final CatalogoRepositoryPort catalogo;
    private final SanitizadorTexto sanitizador;
    private final Reloj reloj;
    private final FlotaMapper mapper;

    public GestionIncidenciasService(IncidenciaRepositoryPort incidenciaRepositorio,
                                     VehiculoRepositoryPort vehiculoRepositorio,
                                     RutaRepositoryPort rutaRepositorio,
                                     PqrsRepositoryPort pqrsRepositorio,
                                     UsuarioRepositoryPort usuarioRepositorio,
                                     CatalogoRepositoryPort catalogo,
                                     SanitizadorTexto sanitizador,
                                     Reloj reloj,
                                     FlotaMapper mapper) {
        this.incidenciaRepositorio = incidenciaRepositorio;
        this.vehiculoRepositorio = vehiculoRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.pqrsRepositorio = pqrsRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.catalogo = catalogo;
        this.sanitizador = sanitizador;
        this.reloj = reloj;
        this.mapper = mapper;
    }

    @Transactional
    public IncidenciaRespuesta registrar(RegistrarIncidenciaPeticion peticion, String nombreUsuario) {
        LocalDateTime ahora = reloj.ahora();

        if (peticion.fechaHora().isAfter(ahora)) {
            throw new OperacionNoPermitidaException("La incidencia no puede estar fechada en el futuro");
        }

        Vehiculo vehiculo = vehiculoRepositorio.buscarPorPlaca(peticion.placaVehiculo().toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("vehículo", peticion.placaVehiculo()));

        TipoIncidencia tipo = catalogo.tipoIncidenciaPorCodigo(peticion.tipoIncidencia())
                .orElseThrow(() -> new RecursoNoEncontradoException("tipo de incidencia", peticion.tipoIncidencia()));

        Ruta ruta = null;
        if (sanitizador.nuloSiVacio(peticion.codigoRuta()) != null) {
            ruta = rutaRepositorio.buscarPorCodigo(peticion.codigoRuta())
                    .orElseThrow(() -> new RecursoNoEncontradoException("ruta", peticion.codigoRuta()));
        }

        Incidencia incidencia = new Incidencia(
                siguienteConsecutivo(ahora.getYear()),
                vehiculo,
                ruta,
                tipo,
                usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario).orElse(null),
                sanitizador.limpiar(peticion.descripcion()),
                peticion.fechaHora());

        if (sanitizador.nuloSiVacio(peticion.radicadoAsociado()) != null) {
            incidencia.vincularConSolicitud(
                    pqrsRepositorio.buscarPorRadicado(peticion.radicadoAsociado())
                            .orElseThrow(() -> new RecursoNoEncontradoException(
                                    "solicitud", peticion.radicadoAsociado())));
        }

        return mapper.aIncidencia(incidenciaRepositorio.guardar(incidencia));
    }

    @Transactional
    public IncidenciaRespuesta tomarEnAtencion(String consecutivo) {
        Incidencia incidencia = obtener(consecutivo);
        incidencia.tomarEnAtencion();
        return mapper.aIncidencia(incidenciaRepositorio.guardar(incidencia));
    }

    @Transactional
    public IncidenciaRespuesta cerrar(String consecutivo) {
        Incidencia incidencia = obtener(consecutivo);
        incidencia.cerrar();
        return mapper.aIncidencia(incidenciaRepositorio.guardar(incidencia));
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<IncidenciaRespuesta> listar(Paginacion paginacion) {
        return PaginaRespuesta.desde(incidenciaRepositorio.listar(paginacion).mapear(mapper::aIncidencia));
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<IncidenciaRespuesta> listarPorVehiculo(String placa, Paginacion paginacion) {
        return PaginaRespuesta.desde(
                incidenciaRepositorio.listarPorVehiculo(placa.toUpperCase(), paginacion)
                        .mapear(mapper::aIncidencia));
    }

    private Incidencia obtener(String consecutivo) {
        return incidenciaRepositorio.buscarPorConsecutivo(consecutivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("incidencia", consecutivo));
    }

    private String siguienteConsecutivo(int anio) {
        long yaRegistradas = incidenciaRepositorio.contarDelAnio(anio);
        return "%s-%d-%05d".formatted(PREFIJO_CONSECUTIVO, anio, yaRegistradas + 1);
    }
}
