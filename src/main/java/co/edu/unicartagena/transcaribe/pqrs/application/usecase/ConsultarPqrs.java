package co.edu.unicartagena.transcaribe.pqrs.application.usecase;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsResumenRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.SeguimientoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import java.util.List;

/** Caso de uso: consultar solicitudes y su trazabilidad. */
public interface ConsultarPqrs {

    /** Detalle completo; solo para funcionarios autenticados. */
    PqrsRespuesta porRadicado(String radicado);

    /**
     * Consulta pública del ciudadano: exige radicado y documento, y devuelve
     * los datos personales enmascarados.
     *
     * <p>Pedir los dos datos a la vez es lo que impide que conocer un radicado
     * baste para leer la solicitud de otra persona.</p>
     */
    PqrsRespuesta consultaCiudadana(String radicado, String numeroDocumento);

    PaginaRespuesta<PqrsResumenRespuesta> buscar(FiltroPqrs filtro, Paginacion paginacion);

    List<SeguimientoRespuesta> trazabilidad(String radicado);
}
