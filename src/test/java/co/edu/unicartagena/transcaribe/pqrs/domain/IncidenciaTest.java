package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.EstadoIncidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Incidencia de flota")
class IncidenciaTest {

    private static final LocalDateTime HECHO = LocalDateTime.of(2026, 3, 4, 7, 45);

    private Incidencia nuevaIncidencia() {
        return new Incidencia(
                "INC-2026-00001",
                FabricaPruebas.vehiculo("UXT101"),
                FabricaPruebas.ruta("T100"),
                FabricaPruebas.tipoIncidencia("FALLA_MECANICA", Prioridad.ALTA),
                FabricaPruebas.usuario("jlopez"),
                "El bus se detuvo por falla en el sistema de frenos.",
                HECHO);
    }

    @Test
    @DisplayName("nace en estado REPORTADA")
    void naceReportada() {
        assertThat(nuevaIncidencia().getEstado()).isEqualTo(EstadoIncidencia.REPORTADA);
    }

    @Test
    @DisplayName("hereda la prioridad de su tipo")
    void heredaPrioridadDelTipo() {
        assertThat(nuevaIncidencia().prioridad()).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("recorre el ciclo reportada, en atención y cerrada")
    void cicloCompleto() {
        Incidencia incidencia = nuevaIncidencia();

        incidencia.tomarEnAtencion();
        assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.EN_ATENCION);

        incidencia.cerrar();
        assertThat(incidencia.getEstado()).isEqualTo(EstadoIncidencia.CERRADA);
        assertThat(incidencia.getEstado().esFinal()).isTrue();
    }

    @Test
    @DisplayName("no se puede cerrar sin pasar por atención")
    void noSeCierraSinAtencion() {
        assertThatThrownBy(() -> nuevaIncidencia().cerrar())
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("en atención");
    }

    @Test
    @DisplayName("no se puede tomar en atención dos veces")
    void noSeTomaDosVeces() {
        Incidencia incidencia = nuevaIncidencia();
        incidencia.tomarEnAtencion();

        assertThatThrownBy(incidencia::tomarEnAtencion)
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("se puede vincular a la solicitud del ciudadano que presenció el hecho")
    void vinculaConSolicitud() {
        Incidencia incidencia = nuevaIncidencia();

        incidencia.vincularConSolicitud(FabricaPruebas.pqrsRadicada());

        assertThat(incidencia.getPqrs()).isNotNull();
        assertThat(incidencia.getPqrs().getRadicado()).isEqualTo("TC-2026-000001");
    }

    @Test
    @DisplayName("una incidencia cerrada ya no admite vínculos nuevos")
    void cerradaNoAdmiteVinculos() {
        Incidencia incidencia = nuevaIncidencia();
        incidencia.tomarEnAtencion();
        incidencia.cerrar();

        assertThatThrownBy(() -> incidencia.vincularConSolicitud(FabricaPruebas.pqrsRadicada()))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    @DisplayName("el vehículo expone la capacidad nominal de su tipo")
    void capacidadDelVehiculo() {
        assertThat(FabricaPruebas.vehiculo("UXT101").capacidadNominal()).isEqualTo(160);
        assertThat(FabricaPruebas.vehiculo("UXT101").prestaServicio()).isTrue();
    }

    @Test
    @DisplayName("la placa se normaliza a mayúsculas al vincular el vehículo")
    void placaEnMayusculas() {
        assertThat(FabricaPruebas.vehiculo("uxt101").getPlaca()).isEqualTo("UXT101");
    }
}
