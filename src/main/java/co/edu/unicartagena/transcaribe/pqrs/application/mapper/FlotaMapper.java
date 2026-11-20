package co.edu.unicartagena.transcaribe.pqrs.application.mapper;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.IncidenciaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.RutaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.VehiculoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import org.springframework.stereotype.Component;

/**
 * Traduce las entidades de flota a sus vistas de API.
 *
 * <p>Se agrupan ruta, vehículo e incidencia en un solo mapeador porque
 * pertenecen al mismo subdominio y cambian juntas. Repartirlas en tres clases
 * de ocho líneas no haría el código más cohesivo, solo más disperso.</p>
 */
@Component
public class FlotaMapper {

    public VehiculoRespuesta aVehiculo(Vehiculo vehiculo) {
        return new VehiculoRespuesta(
                vehiculo.getPlaca(),
                vehiculo.getNumeroInterno(),
                vehiculo.getTipoVehiculo().getNombre(),
                vehiculo.capacidadNominal(),
                vehiculo.getEstadoOperativo().name(),
                vehiculo.getFechaVinculacion());
    }

    public RutaRespuesta aRuta(Ruta ruta) {
        return new RutaRespuesta(
                ruta.getCodigo(),
                ruta.getNombre(),
                ruta.getOrigen(),
                ruta.getDestino(),
                ruta.isActiva());
    }

    public IncidenciaRespuesta aIncidencia(Incidencia incidencia) {
        return new IncidenciaRespuesta(
                incidencia.getConsecutivo(),
                incidencia.getVehiculo().getPlaca(),
                incidencia.getRuta() == null ? null : incidencia.getRuta().getCodigo(),
                incidencia.getTipoIncidencia().getNombre(),
                incidencia.prioridad().name(),
                incidencia.getEstado().name(),
                incidencia.getDescripcion(),
                incidencia.getPqrs() == null ? null : incidencia.getPqrs().getRadicado(),
                incidencia.getFechaHora());
    }
}
