package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearCiudadanoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RadicarPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.RadicarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.CanalRecepcion;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.CategoriaPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.EstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoDocumento;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoSolicitud;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CiudadanoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.RutaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.VehiculoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabil;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicado;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.PoliticaPrioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Radicación de solicitudes ciudadanas.
 *
 * <p>Orquesta, no decide. Cada regla vive en su sitio —el plazo en el
 * calendario hábil, la prioridad en la política, las transiciones en el
 * agregado— y este servicio se limita a resolver referencias, pedir los
 * cálculos y persistir dentro de una transacción. Esa división es lo que
 * permite que el conjunto de reglas se pruebe sin levantar Spring.</p>
 *
 * <p>Implementa el flujo del caso de uso CU-01 del documento de análisis.</p>
 */
@Service
public class RadicacionPqrsService implements RadicarPqrs {

    private static final Logger LOG = LoggerFactory.getLogger(RadicacionPqrsService.class);

    private final PqrsRepositoryPort pqrsRepositorio;
    private final CiudadanoRepositoryPort ciudadanoRepositorio;
    private final RutaRepositoryPort rutaRepositorio;
    private final VehiculoRepositoryPort vehiculoRepositorio;
    private final CatalogoRepositoryPort catalogo;
    private final GeneradorRadicado generadorRadicado;
    private final PoliticaPrioridad politicaPrioridad;
    private final CalendarioHabil calendario;
    private final Reloj reloj;
    private final SanitizadorTexto sanitizador;
    private final PqrsMapper mapper;

    public RadicacionPqrsService(PqrsRepositoryPort pqrsRepositorio,
                                 CiudadanoRepositoryPort ciudadanoRepositorio,
                                 RutaRepositoryPort rutaRepositorio,
                                 VehiculoRepositoryPort vehiculoRepositorio,
                                 CatalogoRepositoryPort catalogo,
                                 GeneradorRadicado generadorRadicado,
                                 PoliticaPrioridad politicaPrioridad,
                                 CalendarioHabil calendario,
                                 Reloj reloj,
                                 SanitizadorTexto sanitizador,
                                 PqrsMapper mapper) {
        this.pqrsRepositorio = pqrsRepositorio;
        this.ciudadanoRepositorio = ciudadanoRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.vehiculoRepositorio = vehiculoRepositorio;
        this.catalogo = catalogo;
        this.generadorRadicado = generadorRadicado;
        this.politicaPrioridad = politicaPrioridad;
        this.calendario = calendario;
        this.reloj = reloj;
        this.sanitizador = sanitizador;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PqrsRespuesta ejecutar(RadicarPqrsPeticion peticion) {

        TipoSolicitud tipoSolicitud = catalogo.tipoSolicitudPorCodigo(peticion.tipoSolicitud())
                .orElseThrow(() -> new RecursoNoEncontradoException("tipo de solicitud", peticion.tipoSolicitud()));
        CategoriaPqrs categoria = catalogo.categoriaPorCodigo(peticion.categoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("categoría", peticion.categoria()));
        CanalRecepcion canal = catalogo.canalPorCodigo(peticion.canal())
                .orElseThrow(() -> new RecursoNoEncontradoException("canal de recepción", peticion.canal()));
        EstadoPqrs estadoInicial = catalogo.estadoPorCodigo(CodigoEstadoPqrs.RADICADA)
                .orElseThrow(() -> new IllegalStateException("El catálogo de estados no contiene RADICADA"));

        Ciudadano ciudadano = resolverCiudadano(peticion.ciudadano());

        LocalDateTime ahora = reloj.ahora();
        LocalDate hoy = ahora.toLocalDate();

        // RN-01: el término legal corre en días hábiles desde el día siguiente.
        LocalDate vencimiento = calendario.sumarDiasHabiles(hoy, tipoSolicitud.getDiasHabilesRespuesta());

        // RN-06: la prioridad la fija la política, no quien radica.
        Prioridad prioridad = politicaPrioridad.determinar(categoria, tipoSolicitud);

        // RN-02: radicado único y consecutivo dentro del año.
        String radicado = generadorRadicado.generar(hoy.getYear());

        Pqrs pqrs = new Pqrs(
                radicado,
                tipoSolicitud,
                categoria,
                canal,
                estadoInicial,
                ciudadano,
                prioridad,
                sanitizador.limpiar(peticion.asunto()),
                sanitizador.limpiar(peticion.descripcion()),
                ahora,
                vencimiento);

        pqrs.asociarServicio(resolverRuta(peticion.codigoRuta()), resolverVehiculo(peticion.placaVehiculo()));

        Pqrs guardada = pqrsRepositorio.guardar(pqrs);

        LOG.info("Solicitud radicada: radicado={} tipo={} prioridad={} vence={}",
                guardada.getRadicado(), tipoSolicitud.getCodigo(), prioridad, vencimiento);

        return mapper.aDetalle(guardada);
    }

    /**
     * Reutiliza el ciudadano si ya existe y actualiza sus datos de contacto;
     * si no, lo registra. Se evita así duplicar titulares de datos personales
     * cada vez que la misma persona radica una solicitud (RN-07).
     */
    private Ciudadano resolverCiudadano(CrearCiudadanoPeticion datos) {
        return ciudadanoRepositorio
                .buscarPorDocumento(datos.tipoDocumento(), datos.numeroDocumento())
                .map(existente -> {
                    existente.actualizarContacto(
                            sanitizador.limpiar(datos.correo()),
                            sanitizador.nuloSiVacio(datos.telefono()));
                    return ciudadanoRepositorio.guardar(existente);
                })
                .orElseGet(() -> registrarCiudadano(datos));
    }

    private Ciudadano registrarCiudadano(CrearCiudadanoPeticion datos) {
        TipoDocumento tipoDocumento = catalogo.tipoDocumentoPorCodigo(datos.tipoDocumento())
                .orElseThrow(() -> new RecursoNoEncontradoException("tipo de documento", datos.tipoDocumento()));

        Ciudadano nuevo = new Ciudadano(
                tipoDocumento,
                datos.numeroDocumento(),
                sanitizador.limpiar(datos.nombres()),
                sanitizador.limpiar(datos.apellidos()),
                sanitizador.limpiar(datos.correo()),
                sanitizador.nuloSiVacio(datos.telefono()),
                reloj.ahora());

        return ciudadanoRepositorio.guardar(nuevo);
    }

    private Ruta resolverRuta(String codigoRuta) {
        if (sanitizador.nuloSiVacio(codigoRuta) == null) {
            return null;
        }
        return rutaRepositorio.buscarPorCodigo(codigoRuta)
                .orElseThrow(() -> new RecursoNoEncontradoException("ruta", codigoRuta));
    }

    /**
     * RN-05: si la queja señala un vehículo, la placa debe corresponder a uno
     * vinculado a la flota. No se exige que esté operativo: precisamente una
     * queja puede referirse a un bus que acaba de salir de servicio.
     */
    private Vehiculo resolverVehiculo(String placa) {
        if (sanitizador.nuloSiVacio(placa) == null) {
            return null;
        }
        return vehiculoRepositorio.buscarPorPlaca(placa.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("vehículo", placa));
    }
}
