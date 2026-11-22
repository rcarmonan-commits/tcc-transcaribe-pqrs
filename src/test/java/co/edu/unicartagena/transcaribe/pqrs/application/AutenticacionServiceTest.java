package co.edu.unicartagena.transcaribe.pqrs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.AutenticacionPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.AutenticacionRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.service.AutenticacionService;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.AuditoriaPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.security.ProveedorJwt;
import java.lang.reflect.Field;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pruebas de autenticación.
 *
 * <p>Verifican los tres controles de OWASP A07:2021 que implementa el
 * servicio: mensaje de error indistinguible, bloqueo por intentos fallidos y
 * registro de auditoría de cada intento.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Caso de uso: iniciar sesión")
class AutenticacionServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 10, 8, 0);
    private static final String CONTRASENA = "Transc@ribe2026.Fun";
    private static final String IP = "192.168.1.50";

    @Mock private UsuarioRepositoryPort usuarioRepositorio;
    @Mock private AuditoriaPort auditoria;

    private AutenticacionService servicio;
    private PasswordEncoder codificador;

    @BeforeEach
    void prepararEscenario() {
        codificador = new BCryptPasswordEncoder(4); // coste bajo: las pruebas no miden fuerza bruta

        PropiedadesSeguridad propiedades = new PropiedadesSeguridad(
                new PropiedadesSeguridad.Jwt(
                        "clave-de-pruebas-hs256-con-longitud-suficiente-32", 30, "transcaribe-pqrs"),
                3,
                15,
                new PropiedadesSeguridad.Cors(List.of("http://localhost:4200")),
                new PropiedadesSeguridad.LimitePeticiones(false, 60));

        servicio = new AutenticacionService(
                usuarioRepositorio, codificador, new ProveedorJwt(propiedades),
                auditoria, propiedades, (Reloj) () -> AHORA);
    }

    @Test
    @DisplayName("devuelve un testigo con los roles del funcionario")
    void autenticacionCorrecta() {
        Usuario usuario = usuarioConContrasena(CONTRASENA);
        usuario.asignarRol(FabricaPruebas.rol("ROLE_FUNCIONARIO"));
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez")).thenReturn(Optional.of(usuario));

        AutenticacionRespuesta respuesta = servicio.iniciarSesion(
                new AutenticacionPeticion("jlopez", CONTRASENA), IP);

        assertThat(respuesta.token()).isNotBlank();
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.roles()).containsExactly("ROLE_FUNCIONARIO");
        assertThat(respuesta.expiraEn()).isEqualTo(AHORA.plusMinutes(30));
        verify(auditoria).registrar(eq("jlopez"), eq("INICIO_SESION"), anyString(), eq(IP), eq(true));
    }

    @Test
    @DisplayName("un usuario inexistente y una contraseña errónea dan el mismo error")
    void errorIndistinguible() {
        when(usuarioRepositorio.buscarPorNombreUsuario("fantasma")).thenReturn(Optional.empty());
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez"))
                .thenReturn(Optional.of(usuarioConContrasena(CONTRASENA)));

        String mensajeUsuarioInexistente = capturarMensaje("fantasma", "loQueSea");
        String mensajeContrasenaErronea = capturarMensaje("jlopez", "contrasenaIncorrecta");

        assertThat(mensajeUsuarioInexistente).isEqualTo(mensajeContrasenaErronea);
    }

    @Test
    @DisplayName("cuenta los intentos fallidos y persiste el usuario")
    void cuentaIntentosFallidos() {
        Usuario usuario = usuarioConContrasena(CONTRASENA);
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.iniciarSesion(
                new AutenticacionPeticion("jlopez", "incorrecta"), IP))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        verify(usuarioRepositorio).guardar(usuario);
        verify(auditoria).registrar(eq("jlopez"), eq("INICIO_SESION_FALLIDO"), anyString(), eq(IP), eq(false));
    }

    @Test
    @DisplayName("bloquea la cuenta al alcanzar el umbral de intentos")
    void bloqueaTrasVariosIntentos() {
        Usuario usuario = usuarioConContrasena(CONTRASENA);
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> servicio.iniciarSesion(
                    new AutenticacionPeticion("jlopez", "incorrecta"), IP))
                    .isInstanceOf(BadCredentialsException.class);
        }

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();

        // Incluso con la contraseña correcta, la cuenta bloqueada no entra.
        assertThatThrownBy(() -> servicio.iniciarSesion(
                new AutenticacionPeticion("jlopez", CONTRASENA), IP))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("una autenticación correcta limpia los intentos fallidos previos")
    void limpiaIntentosPrevios() {
        Usuario usuario = usuarioConContrasena(CONTRASENA);
        usuario.registrarIntentoFallido(3, 15, AHORA);
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez")).thenReturn(Optional.of(usuario));

        servicio.iniciarSesion(new AutenticacionPeticion("jlopez", CONTRASENA), IP);

        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    @DisplayName("un usuario desactivado no puede entrar")
    void usuarioDesactivado() {
        Usuario usuario = usuarioConContrasena(CONTRASENA);
        usuario.desactivar();
        when(usuarioRepositorio.buscarPorNombreUsuario("jlopez")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.iniciarSesion(
                new AutenticacionPeticion("jlopez", CONTRASENA), IP))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("el DTO de credenciales nunca expone la contraseña al imprimirse")
    void dtoNoFiltraLaContrasena() {
        String texto = new AutenticacionPeticion("jlopez", "secreto-real").toString();

        assertThat(texto).contains("jlopez").doesNotContain("secreto-real").contains("***");
    }

    private String capturarMensaje(String usuario, String contrasena) {
        try {
            servicio.iniciarSesion(new AutenticacionPeticion(usuario, contrasena), IP);
            throw new AssertionError("Se esperaba un fallo de autenticación");
        } catch (BadCredentialsException e) {
            return e.getMessage();
        }
    }

    private Usuario usuarioConContrasena(String contrasenaEnClaro) {
        Usuario usuario = FabricaPruebas.usuario("jlopez");
        escribirCampo(usuario, "hashContrasena", codificador.encode(contrasenaEnClaro));
        return usuario;
    }

    private void escribirCampo(Object objeto, String campo, Object valor) {
        try {
            Field atributo = objeto.getClass().getDeclaredField(campo);
            atributo.setAccessible(true);
            atributo.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
