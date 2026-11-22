package co.edu.unicartagena.transcaribe.pqrs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsResumenRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.service.ConsultaPqrsService;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pruebas de consulta, con foco en la consulta pública del ciudadano.
 *
 * <p>El riesgo que cubren es concreto: el radicado es consecutivo y adivinable.
 * Si bastara con conocerlo, cualquiera recorrería los datos personales de
 * terceros cambiando un número (OWASP A01:2021).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Caso de uso: consultar PQRS")
class ConsultaPqrsServiceTest {

    private static final String RADICADO = "TC-2026-000001";
    private static final String DOCUMENTO = "1047398221";

    @Mock private PqrsRepositoryPort pqrsRepositorio;

    private ConsultaPqrsService servicio;

    @BeforeEach
    void prepararEscenario() {
        Reloj reloj = () -> LocalDateTime.of(2026, 3, 10, 9, 0);
        servicio = new ConsultaPqrsService(pqrsRepositorio, new PqrsMapper(reloj));
    }

    @Test
    @DisplayName("el detalle interno devuelve los datos completos del ciudadano")
    void detalleInterno() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        PqrsRespuesta respuesta = servicio.porRadicado(RADICADO);

        assertThat(respuesta.ciudadano().numeroDocumento()).isEqualTo(DOCUMENTO);
        assertThat(respuesta.ciudadano().correo()).isEqualTo("capacheco@correo.com");
    }

    @Test
    @DisplayName("la consulta ciudadana enmascara documento y correo")
    void consultaCiudadanaEnmascara() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        PqrsRespuesta respuesta = servicio.consultaCiudadana(RADICADO, DOCUMENTO);

        assertThat(respuesta.ciudadano().numeroDocumento()).isEqualTo("******8221");
        assertThat(respuesta.ciudadano().correo()).isEqualTo("c********@correo.com");
        assertThat(respuesta.ciudadano().telefono()).isNull();
        assertThat(respuesta.ciudadano().id()).isNull();
        assertThat(respuesta.funcionarioAsignado()).isNull();
    }

    @Test
    @DisplayName("la consulta ciudadana conserva el contenido de la solicitud")
    void consultaCiudadanaConservaElContenido() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        PqrsRespuesta respuesta = servicio.consultaCiudadana(RADICADO, DOCUMENTO);

        assertThat(respuesta.radicado()).isEqualTo(RADICADO);
        assertThat(respuesta.estado()).isEqualTo("RADICADA");
        assertThat(respuesta.asunto()).contains("T100");
    }

    @Test
    @DisplayName("un documento que no corresponde al titular se rechaza")
    void documentoQueNoCorresponde() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        assertThatThrownBy(() -> servicio.consultaCiudadana(RADICADO, "9999999999"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("acertar el radicado sin el documento no se distingue de no acertarlo")
    void noSeDistingueElRadicadoValido() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));
        when(pqrsRepositorio.buscarPorRadicado("TC-2026-999999")).thenReturn(Optional.empty());

        RecursoNoEncontradoException conDocumentoErroneo = errorDe(RADICADO, "9999999999");
        RecursoNoEncontradoException conRadicadoInexistente = errorDe("TC-2026-999999", DOCUMENTO);

        // Mismo tipo y mismo código: quien prueba radicados al azar no puede
        // saber cuáles existen, que es justo lo que haría falta para empezar
        // a adivinar documentos.
        assertThat(conDocumentoErroneo.getCodigo())
                .isEqualTo(conRadicadoInexistente.getCodigo())
                .isEqualTo("RECURSO_NO_ENCONTRADO");

        // Lo único variable del mensaje es el radicado que envió quien consulta,
        // de modo que no revela nada que no supiera ya.
        assertThat(conDocumentoErroneo.getMessage()).contains(RADICADO);
        assertThat(conRadicadoInexistente.getMessage()).contains("TC-2026-999999");
    }

    @Test
    @DisplayName("la trazabilidad devuelve la bitácora en orden")
    void trazabilidad() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        assertThat(servicio.trazabilidad(RADICADO)).hasSize(1);
        assertThat(servicio.trazabilidad(RADICADO).get(0).estadoNuevo()).isEqualTo("RADICADA");
        assertThat(servicio.trazabilidad(RADICADO).get(0).funcionario()).isEqualTo("Sistema");
    }

    @Test
    @DisplayName("el listado devuelve resúmenes con el documento enmascarado")
    void listadoEnmascarado() {
        Pqrs pqrs = FabricaPruebas.pqrsRadicada();
        when(pqrsRepositorio.buscar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new Pagina<>(List.of(pqrs), 0, 20, 1L));

        PaginaRespuesta<PqrsResumenRespuesta> pagina =
                servicio.buscar(FiltroPqrs.sinFiltro(), Paginacion.primeraPagina());

        assertThat(pagina.totalElementos()).isEqualTo(1L);
        assertThat(pagina.contenido().get(0).ciudadanoDocumentoEnmascarado()).isEqualTo("******8221");
    }

    private RecursoNoEncontradoException errorDe(String radicado, String documento) {
        try {
            servicio.consultaCiudadana(radicado, documento);
            throw new AssertionError("Se esperaba un rechazo");
        } catch (RecursoNoEncontradoException e) {
            return e;
        }
    }
}
