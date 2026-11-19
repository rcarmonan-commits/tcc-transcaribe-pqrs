package co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta;

/**
 * Petición de paginación expresada en términos del dominio.
 *
 * <p>El dominio no conoce {@code Pageable} de Spring Data. Traducir en el
 * adaptador cuesta tres líneas y a cambio el núcleo queda sustituible: la
 * misma regla funciona si mañana la persistencia cambia de tecnología
 * (principio de inversión de dependencias).</p>
 *
 * <p>El tamaño máximo es una defensa, no una comodidad: sin tope, una sola
 * petición podría arrastrar la tabla completa y tumbar el servicio.</p>
 */
public record Paginacion(int pagina, int tamano, String ordenarPor, boolean descendente) {

    public static final int TAMANO_MAXIMO = 100;
    public static final int TAMANO_POR_DEFECTO = 20;

    public Paginacion {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (tamano <= 0) {
            tamano = TAMANO_POR_DEFECTO;
        }
        if (tamano > TAMANO_MAXIMO) {
            tamano = TAMANO_MAXIMO;
        }
        if (ordenarPor == null || ordenarPor.isBlank()) {
            ordenarPor = "fechaRadicacion";
        }
    }

    public static Paginacion primeraPagina() {
        return new Paginacion(0, TAMANO_POR_DEFECTO, "fechaRadicacion", true);
    }
}
