package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CambiarEstadoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.ResponderPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.GestionarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.EstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión del ciclo de vida de una solicitud ya radicada (CU-03, CU-04, CU-05).
 *
 * <p>Ninguna de las reglas de transición está escrita en esta clase: el
 * servicio recupera el agregado, le pide el cambio y persiste. Si la transición
 * es ilegal, el agregado lanza antes de que nada se escriba. Esa inversión
 * —la regla en el modelo, la coordinación en el servicio— es lo que evita que
 * la lógica se duplique cada vez que aparece un nuevo punto de entrada.</p>
 */
@Service
public class GestionPqrsService implements GestionarPqrs {

    private static final Logger LOG = LoggerFactory.getLogger(GestionPqrsService.class);

    private final PqrsRepositoryPort pqrsRepositorio;
    private final UsuarioRepositoryPort usuarioRepositorio;
    private final CatalogoRepositoryPort catalogo;
    private final SanitizadorTexto sanitizador;
    private final Reloj reloj;
    private final PqrsMapper mapper;

    public GestionPqrsService(PqrsRepositoryPort pqrsRepositorio,
                              UsuarioRepositoryPort usuarioRepositorio,
                              CatalogoRepositoryPort catalogo,
                              SanitizadorTexto sanitizador,
                              Reloj reloj,
                              PqrsMapper mapper) {
        this.pqrsRepositorio = pqrsRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.catalogo = catalogo;
        this.sanitizador = sanitizador;
        this.reloj = reloj;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PqrsRespuesta cambiarEstado(String radicado, CambiarEstadoPeticion peticion, String nombreUsuario) {
        Pqrs pqrs = obtener(radicado);
        Usuario autor = obtenerUsuario(nombreUsuario);
        EstadoPqrs destino = obtenerEstado(CodigoEstadoPqrs.valueOf(peticion.estadoDestino()));

        pqrs.cambiarEstado(destino, sanitizador.limpiar(peticion.observacion()), autor, reloj.ahora());
        Pqrs guardada = pqrsRepositorio.guardar(pqrs);

        LOG.info("Transición aplicada: radicado={} destino={} funcionario={}",
                sanitizador.paraRegistro(radicado), peticion.estadoDestino(),
                sanitizador.paraRegistro(nombreUsuario));

        return mapper.aDetalle(guardada);
    }

    @Override
    @Transactional
    public PqrsRespuesta responder(String radicado, ResponderPqrsPeticion peticion, String nombreUsuario) {
        Pqrs pqrs = obtener(radicado);
        Usuario autor = obtenerUsuario(nombreUsuario);
        EstadoPqrs resuelta = obtenerEstado(CodigoEstadoPqrs.RESUELTA);

        pqrs.responder(sanitizador.limpiar(peticion.respuesta()), resuelta, autor, reloj.ahora());
        Pqrs guardada = pqrsRepositorio.guardar(pqrs);

        LOG.info("Respuesta registrada: radicado={} funcionario={}",
                sanitizador.paraRegistro(radicado), sanitizador.paraRegistro(nombreUsuario));

        return mapper.aDetalle(guardada);
    }

    @Override
    @Transactional
    public PqrsRespuesta asignar(String radicado, String nombreUsuarioDestino, String nombreUsuarioQueAsigna) {
        Pqrs pqrs = obtener(radicado);
        Usuario destinatario = obtenerUsuario(nombreUsuarioDestino);

        pqrs.asignarA(destinatario);
        Pqrs guardada = pqrsRepositorio.guardar(pqrs);

        LOG.info("Solicitud asignada: radicado={} destinatario={} por={}",
                sanitizador.paraRegistro(radicado),
                sanitizador.paraRegistro(nombreUsuarioDestino),
                sanitizador.paraRegistro(nombreUsuarioQueAsigna));

        return mapper.aDetalle(guardada);
    }

    /**
     * Marca como vencidas las solicitudes cuyo término legal expiró sin
     * respuesta. Lo ejecuta una tarea programada, no una petición: el
     * incumplimiento es un hecho del calendario, no de que alguien consulte.
     *
     * <p>Una solicitud que ya está en un estado desde el que no se puede pasar
     * a VENCIDA se omite en silencio en lugar de interrumpir el lote. Que una
     * fila esté en un estado inesperado no debe impedir marcar las demás.</p>
     */
    @Override
    @Transactional
    public int marcarVencidas() {
        EstadoPqrs vencida = obtenerEstado(CodigoEstadoPqrs.VENCIDA);
        List<Pqrs> candidatas = pqrsRepositorio.buscarVencidasSinRespuesta(reloj.hoy());

        int afectadas = 0;
        for (Pqrs pqrs : candidatas) {
            if (!pqrs.getEstado().codigoEstado().permiteTransicionA(CodigoEstadoPqrs.VENCIDA)) {
                continue;
            }
            pqrs.cambiarEstado(vencida,
                    "Término legal vencido sin respuesta de fondo", null, reloj.ahora());
            pqrsRepositorio.guardar(pqrs);
            afectadas++;
        }

        if (afectadas > 0) {
            LOG.warn("Solicitudes marcadas como vencidas: {}", afectadas);
        }
        return afectadas;
    }

    private Pqrs obtener(String radicado) {
        return pqrsRepositorio.buscarPorRadicado(radicado)
                .orElseThrow(() -> new RecursoNoEncontradoException("solicitud", radicado));
    }

    private Usuario obtenerUsuario(String nombreUsuario) {
        return usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario", nombreUsuario));
    }

    private EstadoPqrs obtenerEstado(CodigoEstadoPqrs codigo) {
        return catalogo.estadoPorCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException(
                        "El catálogo de estados no contiene " + codigo));
    }
}
