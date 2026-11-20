package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Traduce entre la paginación del dominio y la de Spring Data.
 *
 * <p>El detalle que importa es la lista blanca de campos de ordenación. El
 * nombre del campo llega de un parámetro de consulta y acaba convertido en una
 * ruta de propiedad JPA; si se acepta cualquier cadena, un cliente puede
 * ordenar por relaciones no previstas, provocar uniones costosas y, en el peor
 * caso, inferir datos a los que no tiene acceso observando el orden del
 * resultado. Aceptar solo los campos previstos cierra esa puerta.</p>
 */
public final class TraductorPaginacion {

    /** Campos por los que se permite ordenar un listado de solicitudes. */
    private static final Set<String> CAMPOS_PQRS = Set.of(
            "fechaRadicacion", "fechaVencimiento", "prioridad", "radicado", "asunto");

    /** Campos por los que se permite ordenar listados genéricos de flota. */
    private static final Set<String> CAMPOS_FLOTA = Set.of(
            "placa", "numeroInterno", "fechaVinculacion", "fechaHora", "consecutivo");

    private static final String CAMPO_POR_DEFECTO_PQRS = "fechaRadicacion";

    private TraductorPaginacion() {
        // Clase de utilidad.
    }

    public static Pageable aPageablePqrs(Paginacion paginacion) {
        return construir(paginacion, CAMPOS_PQRS, CAMPO_POR_DEFECTO_PQRS);
    }

    public static Pageable aPageableFlota(Paginacion paginacion, String campoPorDefecto) {
        return construir(paginacion, CAMPOS_FLOTA, campoPorDefecto);
    }

    public static <T> Pagina<T> aPagina(Page<T> page) {
        return new Pagina<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    private static Pageable construir(Paginacion paginacion, Set<String> permitidos, String porDefecto) {
        String campo = permitidos.contains(paginacion.ordenarPor())
                ? paginacion.ordenarPor()
                : porDefecto;

        Sort orden = paginacion.descendente()
                ? Sort.by(campo).descending()
                : Sort.by(campo).ascending();

        return PageRequest.of(paginacion.pagina(), paginacion.tamano(), orden);
    }
}
