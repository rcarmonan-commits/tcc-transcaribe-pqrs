package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabil;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabilColombiano;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas del cálculo de días hábiles.
 *
 * <p>Es la regla con más capacidad de hacer daño del sistema: si cuenta de
 * menos, el servicio declara vencidas solicitudes que están en término; si
 * cuenta de más, la entidad incumple la ley sin enterarse.</p>
 */
@DisplayName("Calendario hábil colombiano")
class CalendarioHabilColombianoTest {

    /** Festivos reales de marzo y abril de 2026. */
    private static final Set<LocalDate> FESTIVOS_2026 = Set.of(
            LocalDate.of(2026, 3, 23),  // San José, trasladado al lunes
            LocalDate.of(2026, 4, 2),   // Jueves Santo
            LocalDate.of(2026, 4, 3));  // Viernes Santo

    private final CalendarioHabil calendario = new CalendarioHabilColombiano(FESTIVOS_2026);

    @Nested
    @DisplayName("Clasificación de un día")
    class Clasificacion {

        @Test
        @DisplayName("un martes corriente es hábil")
        void martesEsHabil() {
            assertThat(calendario.esHabil(LocalDate.of(2026, 3, 10))).isTrue();
        }

        @ParameterizedTest(name = "{0} no es hábil porque {1}")
        @CsvSource({
                "2026-03-07, es sábado",
                "2026-03-08, es domingo",
                "2026-03-23, es festivo",
                "2026-04-03, es Viernes Santo"
        })
        void diasNoHabiles(LocalDate fecha, String motivo) {
            assertThat(calendario.esHabil(fecha))
                    .as("se esperaba no hábil porque %s", motivo)
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Suma de días hábiles (Ley 1755 de 2015, art. 14)")
    class SumaDeDias {

        @Test
        @DisplayName("el término empieza a contar el día siguiente a la radicación")
        void empiezaElDiaSiguiente() {
            // Lunes 2 de marzo + 1 día hábil = martes 3.
            LocalDate resultado = calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 2), 1);
            assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 3));
        }

        @Test
        @DisplayName("salta el fin de semana")
        void saltaFinDeSemana() {
            // Viernes 6 de marzo + 1 día hábil = lunes 9, no sábado 7.
            LocalDate resultado = calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 6), 1);
            assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 9));
        }

        @Test
        @DisplayName("salta un festivo trasladado al lunes")
        void saltaFestivo() {
            // Viernes 20 de marzo + 1 hábil: el lunes 23 es San José, así que cae el martes 24.
            LocalDate resultado = calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 20), 1);
            assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 24));
        }

        @Test
        @DisplayName("quince días hábiles desde el 2 de marzo vencen el 24 de marzo")
        void plazoDeQuinceDiasHabiles() {
            // Del 3 al 24 de marzo hay 15 hábiles: se descuentan dos fines de
            // semana (7, 8, 14, 15, 21, 22) y el festivo del 23.
            LocalDate resultado = calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 2), 15);
            assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 24));
        }

        @Test
        @DisplayName("diez días hábiles cruzando Semana Santa")
        void plazoCruzandoSemanaSanta() {
            // Desde el lunes 30 de marzo; el 2 y 3 de abril son festivos.
            LocalDate resultado = calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 30), 10);
            assertThat(resultado).isEqualTo(LocalDate.of(2026, 4, 15));
        }

        @Test
        @DisplayName("rechaza un plazo de cero o negativo")
        void rechazaPlazoNoPositivo() {
            assertThatThrownBy(() -> calendario.sumarDiasHabiles(LocalDate.of(2026, 3, 2), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mayores que cero");
        }
    }

    @Nested
    @DisplayName("Conteo entre fechas")
    class ConteoEntreFechas {

        @Test
        @DisplayName("cuenta los hábiles de una semana corriente")
        void cuentaSemanaCorriente() {
            long habiles = calendario.diasHabilesEntre(
                    LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 13));
            assertThat(habiles).isEqualTo(4L);
        }

        @Test
        @DisplayName("devuelve cero cuando el rango está invertido")
        void rangoInvertido() {
            long habiles = calendario.diasHabilesEntre(
                    LocalDate.of(2026, 3, 13), LocalDate.of(2026, 3, 9));
            assertThat(habiles).isZero();
        }

        @Test
        @DisplayName("un calendario sin festivos solo descuenta fines de semana")
        void sinFestivos() {
            CalendarioHabil sinFestivos = new CalendarioHabilColombiano(Set.of());
            assertThat(sinFestivos.esHabil(LocalDate.of(2026, 3, 23))).isTrue();
        }
    }
}
