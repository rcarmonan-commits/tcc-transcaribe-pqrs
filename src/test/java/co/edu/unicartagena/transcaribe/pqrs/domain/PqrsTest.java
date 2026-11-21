package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.TransicionInvalidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.SeguimientoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Agregado PQRS")
class PqrsTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 5, 9, 30);
    private static final String JUSTIFICACION_VALIDA =
            "Se cierra tras verificar con el ciudadano que la respuesta fue recibida";

    private final Usuario funcionario = FabricaPruebas.usuario("jlopez");

    @Nested
    @DisplayName("Al radicar")
    class AlRadicar {

        @Test
        @DisplayName("nace en estado RADICADA y deja la primera entrada de la bitácora")
        void dejaTrazaDeCreacion() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();

            assertThat(pqrs.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.RADICADA);
            assertThat(pqrs.getSeguimientos()).hasSize(1);
            assertThat(pqrs.getSeguimientos().get(0).getEstadoAnterior()).isNull();
            assertThat(pqrs.getSeguimientos().get(0).getEstadoNuevo().getCodigo()).isEqualTo("RADICADA");
        }

        @Test
        @DisplayName("rechaza nacer en un estado distinto de RADICADA")
        void rechazaEstadoInicialInvalido() {
            assertThatThrownBy(() -> new Pqrs(
                    "TC-2026-000002",
                    FabricaPruebas.tipoSolicitud("QUEJA", 15),
                    FabricaPruebas.categoria("SERVICIO_RUTA",
                            co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad.MEDIA),
                    FabricaPruebas.canal("WEB"),
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA),
                    FabricaPruebas.ciudadano("1047398221"),
                    co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad.MEDIA,
                    "Asunto de prueba suficiente",
                    "Descripción de prueba con longitud suficiente para pasar.",
                    AHORA,
                    LocalDate.of(2026, 3, 26)))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("RADICADA");
        }

        @Test
        @DisplayName("la lista de seguimientos devuelta no es modificable")
        void seguimientosInmutables() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();

            assertThatThrownBy(() -> pqrs.getSeguimientos().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Al cambiar de estado")
    class AlCambiarEstado {

        @Test
        @DisplayName("aplica una transición válida y la registra con su autor")
        void transicionValida() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();

            SeguimientoPqrs traza = pqrs.cambiarEstado(
                    FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                    "Asignada al área de operaciones", funcionario, AHORA);

            assertThat(pqrs.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.EN_TRAMITE);
            assertThat(pqrs.getSeguimientos()).hasSize(2);
            assertThat(traza.getUsuario()).isEqualTo(funcionario);
            assertThat(traza.getEstadoAnterior().getCodigo()).isEqualTo("RADICADA");
        }

        @Test
        @DisplayName("rechaza una transición que el grafo no permite y no toca el estado")
        void transicionInvalidaNoAlteraElEstado() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();

            assertThatThrownBy(() -> pqrs.cambiarEstado(
                    FabricaPruebas.estado(CodigoEstadoPqrs.CERRADA),
                    JUSTIFICACION_VALIDA, funcionario, AHORA))
                    .isInstanceOf(TransicionInvalidaException.class);

            assertThat(pqrs.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.RADICADA);
            assertThat(pqrs.getSeguimientos()).hasSize(1);
        }

        @Test
        @DisplayName("el cierre exige una justificación de al menos 20 caracteres (RN-10)")
        void cierreExigeJustificacion() {
            Pqrs pqrs = enEstadoResuelta();

            assertThatThrownBy(() -> pqrs.cambiarEstado(
                    FabricaPruebas.estado(CodigoEstadoPqrs.CERRADA), "ok", funcionario, AHORA))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("20");

            assertThat(pqrs.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.RESUELTA);
        }

        @Test
        @DisplayName("una solicitud cerrada no admite ningún cambio (RN-03)")
        void cerradaNoAdmiteCambios() {
            Pqrs pqrs = enEstadoResuelta();
            pqrs.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.CERRADA),
                    JUSTIFICACION_VALIDA, funcionario, AHORA);

            assertThatThrownBy(() -> pqrs.cambiarEstado(
                    FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                    "Reapertura indebida", funcionario, AHORA))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("cerrada");
        }
    }

    @Nested
    @DisplayName("Al responder")
    class AlResponder {

        @Test
        @DisplayName("registra la respuesta, la fecha y deja la solicitud resuelta")
        void respuestaDeFondo() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();
            pqrs.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                    "En gestión", funcionario, AHORA);

            pqrs.responder("Se ajustó la frecuencia de la ruta T100 en hora pico.",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario, AHORA);

            assertThat(pqrs.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.RESUELTA);
            assertThat(pqrs.getRespuesta()).contains("frecuencia");
            assertThat(pqrs.getFechaRespuesta()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("marca la oportunidad como cumplida si responde dentro del término")
        void marcaOportunidadCumplida() {
            Pqrs pqrs = enTramite();

            pqrs.responder("Respuesta emitida dentro del término legal.",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario,
                    LocalDateTime.of(2026, 3, 20, 10, 0));

            assertThat(pqrs.getRespondidaEnPlazo()).isTrue();
        }

        @Test
        @DisplayName("marca la oportunidad como incumplida si responde pasado el término")
        void marcaOportunidadIncumplida() {
            Pqrs pqrs = enTramite();

            pqrs.responder("Respuesta emitida fuera del término legal.",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario,
                    LocalDateTime.of(2026, 3, 30, 10, 0));

            assertThat(pqrs.getRespondidaEnPlazo()).isFalse();
        }

        @Test
        @DisplayName("no se puede responder una solicitud recién radicada")
        void radicadaNoAdmiteRespuesta() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();

            assertThatThrownBy(() -> pqrs.responder("Respuesta prematura con longitud válida.",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario, AHORA))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("no admite respuesta");
        }

        @Test
        @DisplayName("rechaza una respuesta vacía")
        void respuestaVacia() {
            Pqrs pqrs = enTramite();

            assertThatThrownBy(() -> pqrs.responder("   ",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario, AHORA))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("vacía");
        }
    }

    @Nested
    @DisplayName("Cálculo de vencimiento")
    class Vencimiento {

        @Test
        @DisplayName("está vencida cuando pasó la fecha límite sin respuesta")
        void vencidaSinRespuesta() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();
            assertThat(pqrs.estaVencida(LocalDate.of(2026, 3, 24))).isTrue();
        }

        @Test
        @DisplayName("no está vencida el mismo día del vencimiento")
        void elDiaDelVencimientoSigueEnTermino() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();
            assertThat(pqrs.estaVencida(LocalDate.of(2026, 3, 23))).isFalse();
        }

        @Test
        @DisplayName("una solicitud respondida nunca cuenta como vencida")
        void respondidaNoVence() {
            Pqrs pqrs = enTramite();
            pqrs.responder("Respuesta dentro del término con longitud suficiente.",
                    FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA), funcionario,
                    LocalDateTime.of(2026, 3, 10, 9, 0));

            assertThat(pqrs.estaVencida(LocalDate.of(2026, 4, 30))).isFalse();
        }

        @Test
        @DisplayName("los días restantes son negativos tras el vencimiento")
        void diasRestantesNegativos() {
            Pqrs pqrs = FabricaPruebas.pqrsRadicada();
            assertThat(pqrs.diasRestantes(LocalDate.of(2026, 3, 28))).isEqualTo(-5L);
        }
    }

    private Pqrs enTramite() {
        Pqrs pqrs = FabricaPruebas.pqrsRadicada();
        pqrs.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                "En gestión", funcionario, AHORA);
        return pqrs;
    }

    private Pqrs enEstadoResuelta() {
        Pqrs pqrs = enTramite();
        pqrs.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA),
                "Resuelta de fondo", funcionario, AHORA);
        return pqrs;
    }
}
