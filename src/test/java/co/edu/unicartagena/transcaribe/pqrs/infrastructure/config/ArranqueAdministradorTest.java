package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.ArranqueAdministrador.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del arranque de la primera cuenta de administración.
 *
 * <p>La regla que más importa es la primera: si la base ya tiene cuentas, esto
 * no hace nada. Sin esa condición, cada reinicio recrearía una cuenta con una
 * contraseña que quizá alguien ya cambió, lo que sería una puerta trasera
 * disfrazada de comodidad.</p>
 */
@DisplayName("Arranque de la cuenta de administración")
class ArranqueAdministradorTest {

    private static final String USUARIO = "rcarmonan";
    private static final String CORREO = "rcarmonan@unicartagena.edu.co";
    private static final String CONTRASENA_VALIDA = "Transc@ribe2026.Adm";

    @Test
    @DisplayName("si la base ya tiene cuentas, no se crea ninguna")
    void noTocaUnaBaseConCuentas() {
        assertThat(ArranqueAdministrador.decidir(3, USUARIO, CORREO, CONTRASENA_VALIDA))
                .isEqualTo(Decision.YA_HAY_CUENTAS);
    }

    @Test
    @DisplayName("una sola cuenta existente ya basta para no intervenir")
    void unaCuentaBasta() {
        assertThat(ArranqueAdministrador.decidir(1, USUARIO, CORREO, CONTRASENA_VALIDA))
                .isEqualTo(Decision.YA_HAY_CUENTAS);
    }

    @Test
    @DisplayName("sin variables configuradas, el servicio arranca sin crear nada")
    void sinConfiguracionNoCrea() {
        assertThat(ArranqueAdministrador.decidir(0, "", "", ""))
                .isEqualTo(Decision.SIN_CONFIGURAR);
        assertThat(ArranqueAdministrador.decidir(0, USUARIO, CORREO, null))
                .isEqualTo(Decision.SIN_CONFIGURAR);
        assertThat(ArranqueAdministrador.decidir(0, "   ", CORREO, CONTRASENA_VALIDA))
                .isEqualTo(Decision.SIN_CONFIGURAR);
    }

    @Test
    @DisplayName("una contraseña corta no protege una cuenta con todos los permisos")
    void rechazaContrasenaCorta() {
        assertThat(ArranqueAdministrador.decidir(0, USUARIO, CORREO, "corta123"))
                .isEqualTo(Decision.CONTRASENA_CORTA);
    }

    @Test
    @DisplayName("el límite son doce caracteres, y se comprueba en el borde")
    void elLimiteEsDoce() {
        assertThat(ArranqueAdministrador.decidir(0, USUARIO, CORREO, "a".repeat(11)))
                .isEqualTo(Decision.CONTRASENA_CORTA);
        assertThat(ArranqueAdministrador.decidir(0, USUARIO, CORREO, "a".repeat(12)))
                .isEqualTo(Decision.CREAR);
    }

    @Test
    @DisplayName("con la base vacía y todo configurado, procede crearla")
    void creaLaPrimeraCuenta() {
        assertThat(ArranqueAdministrador.decidir(0, USUARIO, CORREO, CONTRASENA_VALIDA))
                .isEqualTo(Decision.CREAR);
    }
}
