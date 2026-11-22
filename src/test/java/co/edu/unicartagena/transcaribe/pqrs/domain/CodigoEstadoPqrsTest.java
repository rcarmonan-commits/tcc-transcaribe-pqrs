package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Máquina de estados de una PQRS (RN-04)")
class CodigoEstadoPqrsTest {

    @ParameterizedTest(name = "{0} puede pasar a {1}")
    @CsvSource({
            "RADICADA, EN_TRAMITE",
            "RADICADA, VENCIDA",
            "EN_TRAMITE, RESUELTA",
            "EN_TRAMITE, VENCIDA",
            "RESUELTA, CERRADA",
            "RESUELTA, EN_TRAMITE",
            "VENCIDA, EN_TRAMITE",
            "VENCIDA, RESUELTA"
    })
    void transicionesPermitidas(CodigoEstadoPqrs origen, CodigoEstadoPqrs destino) {
        assertThat(origen.permiteTransicionA(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} no puede pasar a {1}")
    @CsvSource({
            "RADICADA, RESUELTA",
            "RADICADA, CERRADA",
            "EN_TRAMITE, RADICADA",
            "EN_TRAMITE, CERRADA",
            "RESUELTA, RADICADA",
            "RESUELTA, VENCIDA",
            "CERRADA, EN_TRAMITE",
            "CERRADA, RESUELTA",
            "VENCIDA, CERRADA",
            "VENCIDA, RADICADA"
    })
    void transicionesProhibidas(CodigoEstadoPqrs origen, CodigoEstadoPqrs destino) {
        assertThat(origen.permiteTransicionA(destino)).isFalse();
    }

    @ParameterizedTest(name = "{0} no puede transitar a sí mismo")
    @EnumSource(CodigoEstadoPqrs.class)
    void ningunEstadoTransitaASiMismo(CodigoEstadoPqrs estado) {
        assertThat(estado.permiteTransicionA(estado)).isFalse();
    }

    @ParameterizedTest(name = "{0} rechaza un destino nulo")
    @EnumSource(CodigoEstadoPqrs.class)
    void destinoNuloSiempreSeRechaza(CodigoEstadoPqrs estado) {
        assertThat(estado.permiteTransicionA(null)).isFalse();
    }

    @Test
    @DisplayName("solo CERRADA es estado final")
    void unicoEstadoFinal() {
        assertThat(CodigoEstadoPqrs.CERRADA.esFinal()).isTrue();
        assertThat(CodigoEstadoPqrs.RADICADA.esFinal()).isFalse();
        assertThat(CodigoEstadoPqrs.EN_TRAMITE.esFinal()).isFalse();
        assertThat(CodigoEstadoPqrs.RESUELTA.esFinal()).isFalse();
        assertThat(CodigoEstadoPqrs.VENCIDA.esFinal()).isFalse();
    }

    @Test
    @DisplayName("solo se admite respuesta de fondo desde EN_TRAMITE o VENCIDA")
    void estadosQueAdmitenRespuesta() {
        assertThat(CodigoEstadoPqrs.EN_TRAMITE.admiteRespuesta()).isTrue();
        assertThat(CodigoEstadoPqrs.VENCIDA.admiteRespuesta()).isTrue();
        assertThat(CodigoEstadoPqrs.RADICADA.admiteRespuesta()).isFalse();
        assertThat(CodigoEstadoPqrs.RESUELTA.admiteRespuesta()).isFalse();
        assertThat(CodigoEstadoPqrs.CERRADA.admiteRespuesta()).isFalse();
    }

    @Test
    @DisplayName("el conjunto devuelto no deja modificar la máquina de estados")
    void conjuntoDevueltoEsUnaCopia() {
        var transiciones = CodigoEstadoPqrs.RADICADA.transicionesPermitidas();
        transiciones.add(CodigoEstadoPqrs.CERRADA);

        assertThat(CodigoEstadoPqrs.RADICADA.permiteTransicionA(CodigoEstadoPqrs.CERRADA))
                .as("modificar la copia no debe abrir una transición real")
                .isFalse();
    }
}
