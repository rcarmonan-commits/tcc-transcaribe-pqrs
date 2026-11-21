package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la política de bloqueo por intentos fallidos.
 *
 * <p>Mitiga OWASP A07:2021. Sin un bloqueo, un atacante con una lista de
 * contraseñas comunes solo necesita tiempo.</p>
 */
@DisplayName("Usuario y bloqueo por intentos fallidos")
class UsuarioTest {

    private static final int INTENTOS_MAXIMOS = 3;
    private static final int MINUTOS_BLOQUEO = 15;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 5, 9, 0);

    @Test
    @DisplayName("un usuario nuevo puede autenticarse")
    void usuarioNuevoPuedeEntrar() {
        assertThat(FabricaPruebas.usuario("jlopez").puedeAutenticarse(AHORA)).isTrue();
    }

    @Test
    @DisplayName("no se bloquea antes de alcanzar el umbral")
    void noSeBloqueaAntesDelUmbral() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");

        usuario.registrarIntentoFallido(INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, AHORA);
        usuario.registrarIntentoFallido(INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, AHORA);

        assertThat(usuario.puedeAutenticarse(AHORA)).isTrue();
        assertThat(usuario.getIntentosFallidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("se bloquea al alcanzar el umbral y reinicia el contador")
    void seBloqueaEnElUmbral() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");

        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            usuario.registrarIntentoFallido(INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, AHORA);
        }

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThat(usuario.puedeAutenticarse(AHORA)).isFalse();
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plusMinutes(MINUTOS_BLOQUEO));
    }

    @Test
    @DisplayName("el bloqueo caduca por sí solo")
    void elBloqueoCaduca() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");

        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            usuario.registrarIntentoFallido(INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, AHORA);
        }

        LocalDateTime despues = AHORA.plusMinutes(MINUTOS_BLOQUEO + 1);
        assertThat(usuario.estaBloqueado(despues)).isFalse();
        assertThat(usuario.puedeAutenticarse(despues)).isTrue();
    }

    @Test
    @DisplayName("una autenticación correcta limpia contador y bloqueo")
    void autenticacionExitosaLimpiaElEstado() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");
        usuario.registrarIntentoFallido(INTENTOS_MAXIMOS, MINUTOS_BLOQUEO, AHORA);

        usuario.registrarAutenticacionExitosa();

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }

    @Test
    @DisplayName("un usuario desactivado no puede autenticarse aunque no esté bloqueado")
    void usuarioDesactivado() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");
        usuario.desactivar();

        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
        assertThat(usuario.puedeAutenticarse(AHORA)).isFalse();
    }

    @Test
    @DisplayName("el conjunto de roles devuelto no es modificable")
    void rolesInmutables() {
        Usuario usuario = FabricaPruebas.usuario("jlopez");
        usuario.asignarRol(FabricaPruebas.rol("ROLE_FUNCIONARIO"));

        assertThat(usuario.getRoles()).hasSize(1);
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> usuario.getRoles().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
