package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.PoliticaPrioridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Política de priorización (RN-06)")
class PoliticaPrioridadTest {

    private final PoliticaPrioridad politica = new PoliticaPrioridad();

    @Test
    @DisplayName("usa la prioridad base de la categoría cuando el tipo no impone piso")
    void tomaLaPrioridadDeLaCategoria() {
        Prioridad resultado = politica.determinar(
                FabricaPruebas.categoria("INFRAESTRUCTURA", Prioridad.BAJA),
                FabricaPruebas.tipoSolicitud("SUGERENCIA", 15));

        assertThat(resultado).isEqualTo(Prioridad.BAJA);
    }

    @Test
    @DisplayName("una denuncia nunca entra por debajo de ALTA")
    void denunciaElevaLaPrioridad() {
        Prioridad resultado = politica.determinar(
                FabricaPruebas.categoria("INFRAESTRUCTURA", Prioridad.BAJA),
                FabricaPruebas.tipoSolicitud("DENUNCIA", 15));

        assertThat(resultado).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("el piso de la denuncia no rebaja una categoría crítica")
    void noRebajaUnaCategoriaCritica() {
        Prioridad resultado = politica.determinar(
                FabricaPruebas.categoria("SEGURIDAD", Prioridad.CRITICA),
                FabricaPruebas.tipoSolicitud("DENUNCIA", 15));

        assertThat(resultado).isEqualTo(Prioridad.CRITICA);
    }

    @Test
    @DisplayName("exige atención inmediata solo si es crítica y señala un vehículo")
    void atencionInmediata() {
        assertThat(politica.exigeAtencionInmediata(Prioridad.CRITICA, true)).isTrue();
        assertThat(politica.exigeAtencionInmediata(Prioridad.CRITICA, false)).isFalse();
        assertThat(politica.exigeAtencionInmediata(Prioridad.ALTA, true)).isFalse();
    }

    @Test
    @DisplayName("el máximo entre dos prioridades respeta la severidad declarada")
    void maximoEntrePrioridades() {
        assertThat(Prioridad.BAJA.maximo(Prioridad.ALTA)).isEqualTo(Prioridad.ALTA);
        assertThat(Prioridad.CRITICA.maximo(Prioridad.MEDIA)).isEqualTo(Prioridad.CRITICA);
        assertThat(Prioridad.MEDIA.esAlMenos(Prioridad.MEDIA)).isTrue();
        assertThat(Prioridad.MEDIA.esAlMenos(Prioridad.ALTA)).isFalse();
    }
}
