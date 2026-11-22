package co.edu.unicartagena.transcaribe.pqrs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearCiudadanoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RadicarPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.mapper.PqrsMapper;
import co.edu.unicartagena.transcaribe.pqrs.application.service.RadicacionPqrsService;
import co.edu.unicartagena.transcaribe.pqrs.application.service.SanitizadorTexto;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.CiudadanoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.PqrsRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.RutaRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.VehiculoRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabil;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.CalendarioHabilColombiano;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.GeneradorRadicadoConsecutivo;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.PoliticaPrioridad;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pruebas del caso de uso de radicación.
 *
 * <p>Se sustituyen los puertos por dobles y se conservan las piezas de dominio
 * reales —calendario, política de prioridad, agregado—. Sustituir también las
 * reglas dejaría la prueba verificando que el servicio llama a sus
 * colaboradores, que es lo que ya se ve leyendo el código, en lugar de
 * verificar que el resultado es correcto.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Caso de uso: radicar una PQRS")
class RadicacionPqrsServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 2, 10, 0);

    @Mock private PqrsRepositoryPort pqrsRepositorio;
    @Mock private CiudadanoRepositoryPort ciudadanoRepositorio;
    @Mock private RutaRepositoryPort rutaRepositorio;
    @Mock private VehiculoRepositoryPort vehiculoRepositorio;
    @Mock private CatalogoRepositoryPort catalogo;

    private RadicacionPqrsService servicio;

    @BeforeEach
    void prepararEscenario() {
        Reloj reloj = () -> AHORA;

        CalendarioHabil calendario = new CalendarioHabilColombiano(
                Set.of(LocalDate.of(2026, 3, 23)));

        servicio = new RadicacionPqrsService(
                pqrsRepositorio,
                ciudadanoRepositorio,
                rutaRepositorio,
                vehiculoRepositorio,
                catalogo,
                new GeneradorRadicadoConsecutivo(anio -> 7L, "TC"),
                new PoliticaPrioridad(),
                calendario,
                reloj,
                new SanitizadorTexto(),
                new PqrsMapper(reloj));

        when(catalogo.tipoSolicitudPorCodigo("QUEJA"))
                .thenReturn(Optional.of(FabricaPruebas.tipoSolicitud("QUEJA", 15)));
        when(catalogo.tipoSolicitudPorCodigo("DENUNCIA"))
                .thenReturn(Optional.of(FabricaPruebas.tipoSolicitud("DENUNCIA", 15)));
        when(catalogo.categoriaPorCodigo("SERVICIO_RUTA"))
                .thenReturn(Optional.of(FabricaPruebas.categoria("SERVICIO_RUTA", Prioridad.MEDIA)));
        when(catalogo.categoriaPorCodigo("INFRAESTRUCTURA"))
                .thenReturn(Optional.of(FabricaPruebas.categoria("INFRAESTRUCTURA", Prioridad.BAJA)));
        when(catalogo.canalPorCodigo("WEB"))
                .thenReturn(Optional.of(FabricaPruebas.canal("WEB")));
        when(catalogo.estadoPorCodigo(CodigoEstadoPqrs.RADICADA))
                .thenReturn(Optional.of(FabricaPruebas.estado(CodigoEstadoPqrs.RADICADA)));
        when(catalogo.tipoDocumentoPorCodigo("CC"))
                .thenReturn(Optional.of(FabricaPruebas.tipoDocumento("CC")));

        when(ciudadanoRepositorio.buscarPorDocumento(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(ciudadanoRepositorio.guardar(any(Ciudadano.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(pqrsRepositorio.guardar(any(Pqrs.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("calcula el vencimiento sumando días hábiles al día de radicación")
    void calculaVencimientoEnDiasHabiles() {
        PqrsRespuesta respuesta = servicio.ejecutar(peticion("QUEJA", "SERVICIO_RUTA", null, null));

        // Lunes 2 de marzo + 15 hábiles, descontando el festivo del 23, da el 24.
        assertThat(respuesta.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(respuesta.fundamentoLegal()).contains("Ley 1755 de 2015");
    }

    @Test
    @DisplayName("asigna el radicado con el formato institucional")
    void asignaRadicado() {
        PqrsRespuesta respuesta = servicio.ejecutar(peticion("QUEJA", "SERVICIO_RUTA", null, null));

        assertThat(respuesta.radicado()).isEqualTo("TC-2026-000007");
        assertThat(respuesta.estado()).isEqualTo("RADICADA");
    }

    @Test
    @DisplayName("la prioridad la decide la política, no quien radica")
    void prioridadDecididaPorLaPolitica() {
        PqrsRespuesta respuesta = servicio.ejecutar(
                peticion("DENUNCIA", "INFRAESTRUCTURA", null, null));

        // La categoría es de prioridad BAJA, pero una denuncia nunca baja de ALTA.
        assertThat(respuesta.prioridad()).isEqualTo("ALTA");
    }

    @Test
    @DisplayName("registra al ciudadano cuando radica por primera vez")
    void registraCiudadanoNuevo() {
        servicio.ejecutar(peticion("QUEJA", "SERVICIO_RUTA", null, null));

        ArgumentCaptor<Ciudadano> captor = ArgumentCaptor.forClass(Ciudadano.class);
        verify(ciudadanoRepositorio).guardar(captor.capture());

        assertThat(captor.getValue().getNumeroDocumento()).isEqualTo("1047398221");
        assertThat(captor.getValue().getNombres()).isEqualTo("Carlos Andrés");
    }

    @Test
    @DisplayName("reutiliza al ciudadano existente y actualiza su contacto (RN-07)")
    void reutilizaCiudadanoExistente() {
        Ciudadano existente = FabricaPruebas.ciudadano("1047398221");
        when(ciudadanoRepositorio.buscarPorDocumento("CC", "1047398221"))
                .thenReturn(Optional.of(existente));

        servicio.ejecutar(peticion("QUEJA", "SERVICIO_RUTA", null, null));

        verify(catalogo, never()).tipoDocumentoPorCodigo(anyString());
        assertThat(existente.getCorreo()).isEqualTo("capacheco@correo.com");
    }

    @Test
    @DisplayName("asocia la ruta y el vehículo señalados por el ciudadano")
    void asociaServicioReclamado() {
        when(rutaRepositorio.buscarPorCodigo("T100"))
                .thenReturn(Optional.of(FabricaPruebas.ruta("T100")));
        when(vehiculoRepositorio.buscarPorPlaca("UXT101"))
                .thenReturn(Optional.of(FabricaPruebas.vehiculo("UXT101")));

        PqrsRespuesta respuesta = servicio.ejecutar(
                peticion("QUEJA", "SERVICIO_RUTA", "T100", "UXT101"));

        assertThat(respuesta.codigoRuta()).isEqualTo("T100");
        assertThat(respuesta.placaVehiculo()).isEqualTo("UXT101");
    }

    @Test
    @DisplayName("rechaza una placa que no está vinculada a la flota (RN-05)")
    void rechazaPlacaDesconocida() {
        when(vehiculoRepositorio.buscarPorPlaca("XXX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.ejecutar(
                peticion("QUEJA", "SERVICIO_RUTA", null, "XXX999")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("vehículo");

        verify(pqrsRepositorio, never()).guardar(any());
    }

    @Test
    @DisplayName("rechaza un tipo de solicitud que no existe en el catálogo")
    void rechazaTipoDesconocido() {
        when(catalogo.tipoSolicitudPorCodigo("INVENTADO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.ejecutar(
                peticion("INVENTADO", "SERVICIO_RUTA", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(pqrsRepositorio, never()).guardar(any());
    }

    @Test
    @DisplayName("limpia los caracteres de control del texto antes de persistirlo")
    void limpiaElTextoDeEntrada() {
        RadicarPqrsPeticion sucia = new RadicarPqrsPeticion(
                ciudadanoDePrueba(),
                "QUEJA",
                "SERVICIO_RUTA",
                "WEB",
                "Asunto  con\u0000 control",
                "Descripción con  espacios   repetidos y control\u0007 incrustado.",
                null,
                null);

        PqrsRespuesta respuesta = servicio.ejecutar(sucia);

        assertThat(respuesta.asunto()).isEqualTo("Asunto con control");
        assertThat(respuesta.descripcion()).doesNotContain("\u0007");
    }

    private RadicarPqrsPeticion peticion(String tipo, String categoria, String ruta, String placa) {
        return new RadicarPqrsPeticion(
                ciudadanoDePrueba(),
                tipo,
                categoria,
                "WEB",
                "Bus de la ruta T100 no cumple la frecuencia",
                "El bus pasó con cuarenta minutos de retraso en hora pico.",
                ruta,
                placa);
    }

    private CrearCiudadanoPeticion ciudadanoDePrueba() {
        return new CrearCiudadanoPeticion(
                "CC", "1047398221", "Carlos Andrés", "Pacheco",
                "capacheco@correo.com", "3015558841");
    }
}
