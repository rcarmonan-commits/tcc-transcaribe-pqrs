package co.edu.unicartagena.transcaribe.pqrs.domain.service;

import java.time.LocalDate;

/**
 * Calendario de días hábiles aplicable a los plazos legales de respuesta.
 *
 * <p>Se declara como abstracción y no como clase concreta porque el calendario
 * cambia —festivos nuevos, calendarios distintos si el servicio se reutiliza en
 * otra jurisdicción— mientras la regla "sumar N días hábiles" no cambia. La
 * regla depende de esta interfaz, no de una tabla de festivos concreta
 * (principio de inversión de dependencias).</p>
 */
public interface CalendarioHabil {

    /** Un día es hábil si no es sábado, domingo ni festivo nacional. */
    boolean esHabil(LocalDate fecha);

    /**
     * Suma días hábiles a partir del día siguiente a la fecha dada.
     *
     * <p>El conteo arranca al día siguiente porque así lo establece el artículo
     * 14 de la Ley 1755 de 2015: el término corre desde el día siguiente al de
     * la radicación.</p>
     *
     * @param desde      fecha de radicación
     * @param diasHabiles días hábiles a sumar, mayor que cero
     * @return fecha de vencimiento del término
     */
    LocalDate sumarDiasHabiles(LocalDate desde, int diasHabiles);

    /** Días hábiles transcurridos entre dos fechas, sin contar la inicial. */
    long diasHabilesEntre(LocalDate desde, LocalDate hasta);
}
