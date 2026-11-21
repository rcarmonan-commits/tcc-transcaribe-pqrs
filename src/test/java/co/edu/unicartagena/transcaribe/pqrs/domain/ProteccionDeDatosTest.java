package co.edu.unicartagena.transcaribe.pqrs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unicartagena.transcaribe.pqrs.FabricaPruebas;
import co.edu.unicartagena.transcaribe.pqrs.application.service.SanitizadorTexto;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las defensas sobre datos personales y texto libre.
 *
 * <p>Cubren dos controles distintos: el enmascaramiento, que limita qué sale
 * del servidor, y la limpieza de texto, que impide que un valor hostil
 * fabrique líneas falsas en el registro.</p>
 */
@DisplayName("Protección de datos personales y saneamiento de texto")
class ProteccionDeDatosTest {

    @Nested
    @DisplayName("Enmascaramiento de datos del ciudadano")
    class Enmascaramiento {

        @Test
        @DisplayName("el documento deja visibles solo los cuatro últimos dígitos")
        void documentoEnmascarado() {
            Ciudadano ciudadano = FabricaPruebas.ciudadano("1047398221");

            assertThat(ciudadano.documentoEnmascarado()).isEqualTo("******8221");
            assertThat(ciudadano.documentoEnmascarado()).hasSameSizeAs("1047398221");
        }

        @Test
        @DisplayName("un documento muy corto se oculta por completo")
        void documentoCorto() {
            Ciudadano ciudadano = FabricaPruebas.ciudadano("123");
            assertThat(ciudadano.documentoEnmascarado()).isEqualTo("***");
        }

        @Test
        @DisplayName("el correo conserva la inicial y el dominio")
        void correoEnmascarado() {
            Ciudadano ciudadano = FabricaPruebas.ciudadano("1047398221");
            // "capacheco" tiene 9 caracteres: se conserva la inicial y se ocultan los ocho restantes.
            assertThat(ciudadano.correoEnmascarado()).isEqualTo("c********@correo.com");
        }

        @Test
        @DisplayName("el nombre completo concatena nombres y apellidos")
        void nombreCompleto() {
            assertThat(FabricaPruebas.ciudadano("1047398221").nombreCompleto())
                    .isEqualTo("Carlos Andrés Pacheco");
        }
    }

    @Nested
    @DisplayName("Saneamiento de texto libre")
    class Saneamiento {

        private final SanitizadorTexto sanitizador = new SanitizadorTexto();

        @Test
        @DisplayName("elimina caracteres de control y recorta los extremos")
        void limpiaControles() {
            String sucio = "  Reclamo\u0000 por\u0007 demora  ";
            assertThat(sanitizador.limpiar(sucio)).isEqualTo("Reclamo por demora");
        }

        @Test
        @DisplayName("colapsa los espacios repetidos")
        void colapsaEspacios() {
            assertThat(sanitizador.limpiar("Bus      sin      aire")).isEqualTo("Bus sin aire");
        }

        @Test
        @DisplayName("neutraliza los saltos de línea antes de escribir en el registro")
        void evitaInyeccionDeRegistros() {
            String hostil = "usuario\nINFO: sesion iniciada por administrador";

            String seguro = sanitizador.paraRegistro(hostil);

            assertThat(seguro).doesNotContain("\n").doesNotContain("\r");
            assertThat(seguro).startsWith("usuario_");
        }

        @Test
        @DisplayName("recorta los valores demasiado largos destinados al registro")
        void recortaValoresLargos() {
            String largo = "a".repeat(500);
            assertThat(sanitizador.paraRegistro(largo)).hasSizeLessThanOrEqualTo(201);
        }

        @Test
        @DisplayName("no escapa HTML: el almacén guarda el texto real del ciudadano")
        void noEscapaHtml() {
            String conSimbolos = "Tarifa subió de $2.800 a $3.000 & no avisaron";
            assertThat(sanitizador.limpiar(conSimbolos)).isEqualTo(conSimbolos);
        }

        @Test
        @DisplayName("convierte en nulo un texto en blanco")
        void nuloSiVacio() {
            assertThat(sanitizador.nuloSiVacio("   ")).isNull();
            assertThat(sanitizador.nuloSiVacio(null)).isNull();
            assertThat(sanitizador.nuloSiVacio(" T100 ")).isEqualTo("T100");
        }

        @Test
        @DisplayName("tolera la entrada nula sin fallar")
        void toleraNulos() {
            assertThat(sanitizador.limpiar(null)).isNull();
            assertThat(sanitizador.paraRegistro(null)).isEmpty();
        }
    }
}
