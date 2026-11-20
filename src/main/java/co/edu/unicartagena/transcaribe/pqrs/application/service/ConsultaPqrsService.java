package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsResumenRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.SeguimientoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.ConsultarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas sobre solicitudes (CU-02, CU-06).
 *
 * <p>La operación delicada aquí es la consulta ciudadana. El radicado es un
 * número consecutivo y, por tanto, adivinable: si bastara con conocerlo para
 * leer la solicitud, cualquiera podría recorrer los datos personales de otros
 * recorriendo números. Por eso se exige también el documento del titular y se
 * devuelven los datos enmascarados.</p>
 */
@Service
public class ConsultaPqrsService implements ConsultarPqrs {

    private final PqrsRepositoryPort pqrsRepositorio;
    private final PqrsMapper mapper;

    public ConsultaPqrsService(PqrsRepositoryPort pqrsRepositorio, PqrsMapper mapper) {
        this.pqrsRepositorio = pqrsRepositorio;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PqrsRespuesta porRadicado(String radicado) {
        return mapper.aDetalle(obtener(radicado));
    }

    @Override
    @Transactional(readOnly = true)
    public PqrsRespuesta consultaCiudadana(String radicado, String numeroDocumento) {
        Pqrs pqrs = obtener(radicado);

        // Comparación en tiempo constante: una comparación que corta en el
        // primer carácter distinto filtra información por el tiempo de
        // respuesta y permite reconstruir el documento dígito a dígito.
        boolean coincide = comparacionSegura(
                pqrs.getCiudadano().getNumeroDocumento(), numeroDocumento);

        if (!coincide) {
            // Mismo error que cuando el radicado no existe: distinguir ambos
            // casos confirmaría al atacante qué radicados son válidos.
            throw new RecursoNoEncontradoException("solicitud", radicado);
        }

        PqrsRespuesta detalle = mapper.aDetalle(pqrs);
        return new PqrsRespuesta(
                detalle.radicado(), detalle.tipoSolicitud(), detalle.categoria(), detalle.canal(),
                detalle.estado(), detalle.prioridad(), detalle.asunto(), detalle.descripcion(),
                detalle.respuesta(), mapper.aCiudadanoEnmascarado(pqrs.getCiudadano()),
                detalle.codigoRuta(), detalle.placaVehiculo(), null,
                detalle.fechaRadicacion(), detalle.fechaVencimiento(), detalle.fechaRespuesta(),
                detalle.vencida(), detalle.diasRestantes(), detalle.fundamentoLegal());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaRespuesta<PqrsResumenRespuesta> buscar(FiltroPqrs filtro, Paginacion paginacion) {
        Pagina<Pqrs> pagina = pqrsRepositorio.buscar(filtro, paginacion);
        return PaginaRespuesta.desde(pagina.mapear(mapper::aResumen));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeguimientoRespuesta> trazabilidad(String radicado) {
        return obtener(radicado).getSeguimientos().stream()
                .map(mapper::aSeguimiento)
                .toList();
    }

    private Pqrs obtener(String radicado) {
        return pqrsRepositorio.buscarPorRadicado(radicado)
                .orElseThrow(() -> new RecursoNoEncontradoException("solicitud", radicado));
    }

    private boolean comparacionSegura(String esperado, String recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                recibido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
