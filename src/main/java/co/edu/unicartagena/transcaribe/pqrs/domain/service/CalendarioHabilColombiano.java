package co.edu.unicartagena.transcaribe.pqrs.domain.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Implementación del calendario hábil colombiano.
 *
 * <p>Recibe el conjunto de festivos ya resuelto —con los traslados de la Ley 51
 * de 1983 aplicados— en lugar de calcularlo. Esa separación es deliberada:
 * aquí vive la aritmética de días hábiles, que es estable y fácil de probar, y
 * fuera vive el calendario, que cambia cada año.</p>
 *
 * <p>La clase es inmutable y sin estado mutable compartido, por lo que puede
 * usarse desde varios hilos sin sincronización.</p>
 */
public class CalendarioHabilColombiano implements CalendarioHabil {

    /** Tope de seguridad: evita un bucle infinito si llegara un calendario absurdo. */
    private static final int MAXIMO_DIAS_A_RECORRER = 3650;

    private final Set<LocalDate> festivos;

    public CalendarioHabilColombiano(Set<LocalDate> festivos) {
        this.festivos = Collections.unmodifiableSet(
                Set.copyOf(Objects.requireNonNull(festivos, "festivos")));
    }

    @Override
    public boolean esHabil(LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return !esFinDeSemana(fecha) && !festivos.contains(fecha);
    }

    @Override
    public LocalDate sumarDiasHabiles(LocalDate desde, int diasHabiles) {
        Objects.requireNonNull(desde, "desde");
        if (diasHabiles <= 0) {
            throw new IllegalArgumentException("Los días hábiles a sumar deben ser mayores que cero");
        }

        LocalDate cursor = desde;
        int sumados = 0;
        int recorridos = 0;

        while (sumados < diasHabiles) {
            cursor = cursor.plusDays(1);
            if (++recorridos > MAXIMO_DIAS_A_RECORRER) {
                throw new IllegalStateException(
                        "El calendario de festivos no permite completar el término solicitado");
            }
            if (esHabil(cursor)) {
                sumados++;
            }
        }
        return cursor;
    }

    @Override
    public long diasHabilesEntre(LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");
        if (!hasta.isAfter(desde)) {
            return 0L;
        }

        long habiles = 0L;
        LocalDate cursor = desde.plusDays(1);
        while (!cursor.isAfter(hasta)) {
            if (esHabil(cursor)) {
                habiles++;
            }
            cursor = cursor.plusDays(1);
        }
        return habiles;
    }

    private boolean esFinDeSemana(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }
}
