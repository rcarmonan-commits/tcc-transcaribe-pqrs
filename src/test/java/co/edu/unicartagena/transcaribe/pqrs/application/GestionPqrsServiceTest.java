package co.edu.unicartagena.transcaribe.pqrs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CambiarEstadoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.ResponderPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.service.GestionPqrsService;
import co.edu.unicartagena.transcaribe.pqrs.application.service.SanitizadorTexto;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.TransicionInvalidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.LocalDate;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Caso de uso: gestionar una PQRS")
class GestionPqrsServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 10, 11, 0);
    private static final String RADICADO = "TC-2026-000001";

    @Mock private PqrsRepositoryPort pqrsRepositorio;
    @Mock private UsuarioRepositoryPort usuarioRepositorio;
    @Mock private CatalogoRepositoryPort catalogo;

    private GestionPqrsService servicio;

    @BeforeEach
    void prepararEscenario() {
        Reloj reloj = () -> AHORA;

        servicio = new GestionPqrsService(
                pqrsRepositorio, usuarioRepositorio, catalogo,
                new SanitizadorTexto(), reloj, new PqrsMapper(reloj));

        for (CodigoEstadoPqrs codigo : CodigoEstadoPqrs.values()) {
            when(catalogo.estadoPorCodigo(codigo))
                    .thenReturn(Optional.of(FabricaPruebas.estado(codigo)));
        }
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez"))
                .thenReturn(Optional.of(FabricaPruebas.usuario("jlopez")));
        when(pqrsRepositorio.guardar(any(Pqrs.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("aplica una transición válida y deja la traza con el funcionario")
    void transicionValida() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        PqrsRespuesta respuesta = servicio.cambiarEstado(
                RADICADO, new CambiarEstadoPeticion("EN_TRAMITE", "Asignada a operaciones"), "jlopez");

        assertThat(respuesta.estado()).isEqualTo("EN_TRAMITE");
        verify(pqrsRepositorio).guardar(any(Pqrs.class));
    }

    @Test
    @DisplayName("propaga el rechazo del agregado ante una transición ilegal y no persiste")
    void transicionIlegalNoPersiste() {
        when(pqrsRepositorio.buscarPorRadicado(RADICADO))
                .thenReturn(Optional.of(FabricaPruebas.pqrsRadicada()));

        assertThatThrownBy(() -> servicio.cambiarEstado(
                RADICADO,
                new CambiarEstadoPeticion("CERRADA", "Justificación suficientemente larga para cerrar"),
                "jlopez"))
                .isInstanceOf(TransicionInvalidaException.class);

        verify(pqrsRepositorio, never()).guardar(any());
    }

    @Test
    @DisplayName("falla con un radicado inexistente")
    void radicadoInexistente() {
        when(pqrsRepositorio.buscarPorRadicado("TC-2026-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(
                "TC-2026-999999", new CambiarEstadoPeticion("EN_TRAMITE", "Observación"), "jlopez"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("registra la respuesta de fondo y deja la solicitud resuelta")
    void respondeLaSolicitud() {
        Pqrs enTramite = FabricaPruebas.pqrsRadicada();
        enTramite.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                "En gestión", FabricaPruebas.usuario("jlopez"), AHORA);
        when(pqrsRepositorio.buscarPorRadicado(RADICADO)).thenReturn(Optional.of(enTramite));

        PqrsRespuesta respuesta = servicio.responder(
                RADICADO,
                new ResponderPqrsPeticion("Se ajustó la frecuencia de la ruta T100 en hora pico."),
                "jlopez");

        assertThat(respuesta.estado()).isEqualTo("RESUELTA");
        assertThat(respuesta.respuesta()).contains("frecuencia");
        assertThat(respuesta.fechaRespuesta()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("marca como vencidas las solicitudes fuera de término")
    void marcaVencidas() {
        Pqrs vencida = FabricaPruebas.pqrsRadicada(
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDate.of(2026, 1, 27));
        when(pqrsRepositorio.buscarVencidasSinRespuesta(AHORA.toLocalDate()))
                .thenReturn(List.of(vencida));

        int afectadas = servicio.marcarVencidas();

        assertThat(afectadas).isEqualTo(1);
        assertThat(vencida.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.VENCIDA);
        assertThat(vencida.getSeguimientos()).hasSize(2);
    }

    @Test
    @DisplayName("omite sin interrumpir las solicitudes que no admiten pasar a VENCIDA")
    void omiteLasQueNoAdmitenLaTransicion() {
        Pqrs resuelta = FabricaPruebas.pqrsRadicada();
        resuelta.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.EN_TRAMITE),
                "En gestión", FabricaPruebas.usuario("jlopez"), AHORA);
        resuelta.cambiarEstado(FabricaPruebas.estado(CodigoEstadoPqrs.RESUELTA),
                "Resuelta", FabricaPruebas.usuario("jlopez"), AHORA);

        Pqrs pendiente = FabricaPruebas.pqrsRadicada(
                LocalDateTime.of(2026, 1, 5, 9, 0), LocalDate.of(2026, 1, 27));

        when(pqrsRepositorio.buscarVencidasSinRespuesta(AHORA.toLocalDate()))
                .thenReturn(List.of(resuelta, pendiente));

        int afectadas = servicio.marcarVencidas();

        assertThat(afectadas).isEqualTo(1);
        assertThat(resuelta.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.RESUELTA);
        assertThat(pendiente.getEstado().codigoEstado()).isEqualTo(CodigoEstadoPqrs.VENCIDA);
    }

    @Test
    @DisplayName("no hace nada si no hay solicitudes vencidas")
    void sinVencidas() {
        when(pqrsRepositorio.buscarVencidasSinRespuesta(AHORA.toLocalDate())).thenReturn(List.of());

        assertThat(servicio.marcarVencidas()).isZero();
        verify(pqrsRepositorio, never()).guardar(any());
    }
}
