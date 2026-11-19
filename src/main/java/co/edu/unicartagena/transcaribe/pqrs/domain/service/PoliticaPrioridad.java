package co.edu.unicartagena.transcaribe.pqrs.domain.service;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.CategoriaPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.TipoSolicitud;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import java.util.Objects;
import java.util.Set;

/**
 * Determina con qué prioridad entra una solicitud al flujo de atención (RN-06).
 *
 * <p>La política combina dos señales y se queda con la más severa:</p>
 * <ol>
 *   <li>la prioridad base que declara la categoría temática;</li>
 *   <li>un piso mínimo asociado al tipo de solicitud: una denuncia nunca entra
 *       por debajo de ALTA, por grave que el catálogo considere su categoría.</li>
 * </ol>
 *
 * <p>Está aislada en su propia clase, y no incrustada en el caso de uso, porque
 * es la parte del sistema que más probablemente cambie: basta sustituir esta
 * implementación para endurecer el criterio sin tocar el flujo de radicación
 * (responsabilidad única y abierto/cerrado).</p>
 */
public class PoliticaPrioridad {

    /** Tipos de solicitud que arrastran un piso de prioridad propio. */
    private static final Set<String> TIPOS_CON_PISO_ALTO = Set.of("DENUNCIA");

    private static final Prioridad PISO_DENUNCIA = Prioridad.ALTA;

    public Prioridad determinar(CategoriaPqrs categoria, TipoSolicitud tipoSolicitud) {
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(tipoSolicitud, "tipoSolicitud");

        Prioridad resultado = categoria.getPrioridadBase();

        if (TIPOS_CON_PISO_ALTO.contains(tipoSolicitud.getCodigo())) {
            resultado = resultado.maximo(PISO_DENUNCIA);
        }
        return resultado;
    }

    /**
     * Una solicitud que señala un vehículo concreto y además es crítica merece
     * atención inmediata: es el caso de un accidente o un hecho de seguridad.
     */
    public boolean exigeAtencionInmediata(Prioridad prioridad, boolean tieneVehiculoAsociado) {
        return prioridad.esAlMenos(Prioridad.CRITICA) && tieneVehiculoAsociado;
    }
}
