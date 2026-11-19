package co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta;

import java.util.List;

/**
 * Porción de un resultado de consulta, independiente del framework.
 *
 * @param contenido      elementos de esta página
 * @param pagina         índice de página, base cero
 * @param tamano         tamaño solicitado
 * @param totalElementos total de coincidencias en el almacén
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        if (tamano == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElementos / tamano);
    }

    public boolean tieneSiguiente() {
        return pagina + 1 < totalPaginas();
    }

    /** Transforma el contenido conservando los metadatos de paginación. */
    public <R> Pagina<R> mapear(java.util.function.Function<T, R> conversion) {
        return new Pagina<>(contenido.stream().map(conversion).toList(), pagina, tamano, totalElementos);
    }

    public static <T> Pagina<T> vacia() {
        return new Pagina<>(List.of(), 0, 0, 0L);
    }
}
