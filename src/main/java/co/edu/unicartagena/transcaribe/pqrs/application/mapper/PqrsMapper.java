package co.edu.unicartagena.transcaribe.pqrs.application.mapper;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.CiudadanoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsResumenRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.SeguimientoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.SeguimientoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Traduce el agregado PQRS a las vistas que publica el API.
 *
 * <p>Vive en la capa de aplicación, no en el dominio: el dominio no debe saber
 * qué forma tiene la respuesta JSON. Y no se delega en una librería de mapeo
 * automático a propósito, porque aquí hay decisiones que un mapeo por nombre no
 * puede tomar —qué se enmascara, qué se omite y qué se calcula al vuelo.</p>
 */
@Component
public class PqrsMapper {

    private final Reloj reloj;

    public PqrsMapper(Reloj reloj) {
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public PqrsRespuesta aDetalle(Pqrs pqrs) {
        return new PqrsRespuesta(
                pqrs.getRadicado(),
                pqrs.getTipoSolicitud().getNombre(),
                pqrs.getCategoria().getNombre(),
                pqrs.getCanal().getNombre(),
                pqrs.getEstado().getCodigo(),
                pqrs.getPrioridad().name(),
                pqrs.getAsunto(),
                pqrs.getDescripcion(),
                pqrs.getRespuesta(),
                aCiudadano(pqrs.getCiudadano()),
                pqrs.getRuta() == null ? null : pqrs.getRuta().getCodigo(),
                pqrs.getVehiculo() == null ? null : pqrs.getVehiculo().getPlaca(),
                pqrs.getUsuarioAsignado() == null ? null : pqrs.getUsuarioAsignado().nombreCompleto(),
                pqrs.getFechaRadicacion(),
                pqrs.getFechaVencimiento(),
                pqrs.getFechaRespuesta(),
                pqrs.estaVencida(reloj.hoy()),
                pqrs.diasRestantes(reloj.hoy()),
                pqrs.getTipoSolicitud().getFundamentoLegal());
    }

    public PqrsResumenRespuesta aResumen(Pqrs pqrs) {
        return new PqrsResumenRespuesta(
                pqrs.getRadicado(),
                pqrs.getTipoSolicitud().getNombre(),
                pqrs.getCategoria().getNombre(),
                pqrs.getEstado().getCodigo(),
                pqrs.getPrioridad().name(),
                pqrs.getAsunto(),
                pqrs.getCiudadano().documentoEnmascarado(),
                pqrs.getFechaRadicacion(),
                pqrs.getFechaVencimiento(),
                pqrs.estaVencida(reloj.hoy()));
    }

    public SeguimientoRespuesta aSeguimiento(SeguimientoPqrs seguimiento) {
        return new SeguimientoRespuesta(
                seguimiento.getEstadoAnterior() == null ? null : seguimiento.getEstadoAnterior().getCodigo(),
                seguimiento.getEstadoNuevo().getCodigo(),
                seguimiento.getObservacion(),
                seguimiento.getUsuario() == null ? "Sistema" : seguimiento.getUsuario().nombreCompleto(),
                seguimiento.getFechaRegistro());
    }

    public CiudadanoRespuesta aCiudadano(Ciudadano ciudadano) {
        return new CiudadanoRespuesta(
                ciudadano.getId(),
                ciudadano.getTipoDocumento().getCodigo(),
                ciudadano.getNumeroDocumento(),
                ciudadano.getNombres(),
                ciudadano.getApellidos(),
                ciudadano.getCorreo(),
                ciudadano.getTelefono(),
                ciudadano.getFechaRegistro());
    }

    /** Versión con los datos personales enmascarados, para consultas públicas. */
    public CiudadanoRespuesta aCiudadanoEnmascarado(Ciudadano ciudadano) {
        return new CiudadanoRespuesta(
                null,
                ciudadano.getTipoDocumento().getCodigo(),
                ciudadano.documentoEnmascarado(),
                ciudadano.getNombres(),
                ciudadano.getApellidos(),
                ciudadano.correoEnmascarado(),
                null,
                ciudadano.getFechaRegistro());
    }
}
