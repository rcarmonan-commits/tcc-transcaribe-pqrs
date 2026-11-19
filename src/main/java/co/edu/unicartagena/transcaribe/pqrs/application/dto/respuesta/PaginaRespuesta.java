package co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import java.util.List;

/**
 * Envoltorio de paginación del API.
 *
 * <p>Se define aquí y no se reutiliza el {@code Page} de Spring Data porque la
 * serialización de ese tipo no es estable entre versiones y su forma JSON
 * arrastra campos internos del framework. Un contrato de API no debe cambiar
 * porque se actualizó una dependencia.</p>
 */
public record PaginaRespuesta<T>(List<T> contenido,
                                 int pagina,
                                 int tamano,
                                 long totalElementos,
                                 int totalPaginas,
                                 boolean tieneSiguiente) {

    public static <T> PaginaRespuesta<T> desde(Pagina<T> pagina) {
        return new PaginaRespuesta<>(
                pagina.contenido(),
                pagina.pagina(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                pagina.tieneSiguiente());
    }
}
