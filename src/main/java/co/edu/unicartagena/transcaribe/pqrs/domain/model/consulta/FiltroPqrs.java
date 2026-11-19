package co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import java.time.LocalDate;

/**
 * Criterios de búsqueda de solicitudes. Todos los campos son opcionales:
 * un {@code null} significa "no filtrar por este criterio".
 *
 * <p>Modelar el filtro como un objeto y no como una lista de parámetros evita
 * que la firma del repositorio crezca cada vez que aparece un criterio nuevo,
 * y permite consultarlo con un único método (principio abierto/cerrado).</p>
 */
public record FiltroPqrs(CodigoEstadoPqrs estado,
                         Prioridad prioridad,
                         String codigoTipoSolicitud,
                         String codigoCategoria,
                         String numeroDocumentoCiudadano,
                         String codigoRuta,
                         String placaVehiculo,
                         LocalDate desde,
                         LocalDate hasta,
                         Boolean soloVencidas) {

    public static FiltroPqrs sinFiltro() {
        return new FiltroPqrs(null, null, null, null, null, null, null, null, null, null);
    }

    public boolean pideSoloVencidas() {
        return Boolean.TRUE.equals(soloVencidas);
    }
}
