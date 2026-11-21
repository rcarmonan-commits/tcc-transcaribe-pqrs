package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicado;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicadoConsecutivo;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Radicado y paginación")
class RadicadoYPaginacionTest {

    @Nested
    @DisplayName("Generación del radicado (RN-02)")
    class Radicado {

        private final AtomicLong contador = new AtomicLong(0);
        private final GeneradorRadicado generador =
                new GeneradorRadicadoConsecutivo(anio -> contador.incrementAndGet(), "TC");

        @Test
        @DisplayName("usa el formato PREFIJO-AÑO-NNNNNN con seis dígitos")
        void formatoDelRadicado() {
            assertThat(generador.generar(2026)).isEqualTo("TC-2026-000001");
        }

        @Test
        @DisplayName("avanza el consecutivo en cada llamada")
        void consecutivoAvanza() {
            generador.generar(2026);
            generador.generar(2026);

            assertThat(generador.generar(2026)).isEqualTo("TC-2026-000003");
        }

        @Test
        @DisplayName("no se queda corto con consecutivos de seis cifras")
        void consecutivoGrande() {
            GeneradorRadicado fijo = new GeneradorRadicadoConsecutivo(anio -> 123_456L, "TC");
            assertThat(fijo.generar(2026)).isEqualTo("TC-2026-123456");
        }

        @Test
        @DisplayName("respeta el prefijo configurado")
        void respetaElPrefijo() {
            GeneradorRadicado otro = new GeneradorRadicadoConsecutivo(anio -> 7L, "TCB");
            assertThat(otro.generar(2027)).isEqualTo("TCB-2027-000007");
        }
    }

    @Nested
    @DisplayName("Paginación del dominio")
    class PaginacionDominio {

        @Test
        @DisplayName("recorta un tamaño de página abusivo al máximo permitido")
        void recortaTamanoExcesivo() {
            Paginacion paginacion = new Paginacion(0, 5_000, "fechaRadicacion", true);
            assertThat(paginacion.tamano()).isEqualTo(Paginacion.TAMANO_MAXIMO);
        }

        @Test
        @DisplayName("sustituye un tamaño no positivo por el valor por defecto")
        void tamanoNoPositivo() {
            assertThat(new Paginacion(0, 0, "fechaRadicacion", true).tamano())
                    .isEqualTo(Paginacion.TAMANO_POR_DEFECTO);
        }

        @Test
        @DisplayName("rechaza una página negativa")
        void paginaNegativa() {
            assertThatThrownBy(() -> new Paginacion(-1, 20, "fechaRadicacion", true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("aplica un campo de ordenación por defecto si llega vacío")
        void campoPorDefecto() {
            assertThat(new Paginacion(0, 20, "  ", true).ordenarPor()).isEqualTo("fechaRadicacion");
        }

        @Test
        @DisplayName("calcula el total de páginas y si hay siguiente")
        void metadatosDePagina() {
            Pagina<String> pagina = new Pagina<>(List.of("a", "b"), 0, 2, 5L);

            assertThat(pagina.totalPaginas()).isEqualTo(3);
            assertThat(pagina.tieneSiguiente()).isTrue();
        }

        @Test
        @DisplayName("la última página no anuncia siguiente")
        void ultimaPagina() {
            Pagina<String> pagina = new Pagina<>(List.of("e"), 2, 2, 5L);
            assertThat(pagina.tieneSiguiente()).isFalse();
        }

        @Test
        @DisplayName("mapear conserva los metadatos de paginación")
        void mapearConservaMetadatos() {
            Pagina<String> original = new Pagina<>(List.of("a", "b"), 1, 2, 10L);

            Pagina<Integer> convertida = original.mapear(String::length);

            assertThat(convertida.contenido()).containsExactly(1, 1);
            assertThat(convertida.pagina()).isEqualTo(1);
            assertThat(convertida.totalElementos()).isEqualTo(10L);
        }

        @Test
        @DisplayName("el contenido de una página no es modificable")
        void contenidoInmutable() {
            Pagina<String> pagina = new Pagina<>(new java.util.ArrayList<>(List.of("a")), 0, 1, 1L);

            assertThatThrownBy(() -> pagina.contenido().add("b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
