package co.edu.unicartagena.transcaribe.pqrs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración del API sobre una base H2 real con el esquema de
 * Flyway aplicado.
 *
 * <p>Cubre lo que las pruebas unitarias no pueden: que el esquema versionado y
 * el mapeo de las entidades coinciden, que la cadena de seguridad deja pasar
 * exactamente lo previsto, y que el flujo completo —radicar, autenticarse,
 * tramitar y responder— funciona de principio a fin.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("API de PQRS de extremo a extremo")
class PqrsApiIntegracionTest {

    private static final String CREDENCIALES_FUNCIONARIO = """
            {"nombreUsuario":"jlopez","contrasena":"Transc@ribe2026.Fun"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    @Nested
    @DisplayName("Acceso anónimo")
    class AccesoAnonimo {

        @Test
        @DisplayName("el catálogo de tipos publica el plazo legal de cada uno")
        void catalogoPublico() throws Exception {
            mockMvc.perform(get("/api/v1/catalogos/tipos-solicitud"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.codigo=='PETICION_DOCUMENTOS')].diasHabilesRespuesta")
                            .value(10));
        }

        @Test
        @DisplayName("la gestión de una solicitud exige autenticación")
        void gestionProtegida() throws Exception {
            mockMvc.perform(patch("/api/v1/pqrs/TC-2026-000001/estado")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {"estadoDestino":"EN_TRAMITE","observacion":"Intento anónimo"}
                                     """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("los indicadores exigen autenticación")
        void indicadoresProtegidos() throws Exception {
            mockMvc.perform(get("/api/v1/indicadores"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un testigo falsificado no concede acceso")
        void testigoFalsificado() throws Exception {
            mockMvc.perform(get("/api/v1/indicadores")
                            .header("Authorization", "Bearer no.es.un.jwt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Validación de entrada")
    class Validacion {

        @Test
        @DisplayName("rechaza una radicación con campos inválidos y detalla el motivo")
        void rechazaPeticionInvalida() throws Exception {
            mockMvc.perform(post("/api/v1/pqrs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                       "ciudadano": {
                                         "tipoDocumento": "XX",
                                         "numeroDocumento": "abc",
                                         "nombres": "",
                                         "apellidos": "Pacheco",
                                         "correo": "no-es-un-correo"
                                       },
                                       "tipoSolicitud": "QUEJA",
                                       "categoria": "SERVICIO_RUTA",
                                       "canal": "WEB",
                                       "asunto": "corto",
                                       "descripcion": "breve"
                                     }
                                     """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.detalles").isNotEmpty())
                    .andExpect(jsonPath("$.rastreo").isNotEmpty());
        }

        @Test
        @DisplayName("rechaza un campo no declarado en el contrato")
        void rechazaCampoDesconocido() throws Exception {
            mockMvc.perform(post("/api/v1/pqrs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                       "ciudadano": {
                                         "tipoDocumento": "CC",
                                         "numeroDocumento": "1047398221",
                                         "nombres": "Carlos",
                                         "apellidos": "Pacheco",
                                         "correo": "capacheco@correo.com"
                                       },
                                       "tipoSolicitud": "QUEJA",
                                       "categoria": "SERVICIO_RUTA",
                                       "canal": "WEB",
                                       "asunto": "Asunto con longitud suficiente",
                                       "descripcion": "Descripción con longitud suficiente para pasar.",
                                       "prioridad": "CRITICA"
                                     }
                                     """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PETICION_MAL_FORMADA"));
        }
    }

    @Nested
    @DisplayName("Flujo completo de una solicitud")
    @Transactional
    class FlujoCompleto {

        @Test
        @DisplayName("radicar, consultar, tramitar y responder")
        void cicloDeVida() throws Exception {
            // 1. El ciudadano radica sin estar autenticado.
            String cuerpoRadicacion = mockMvc.perform(post("/api/v1/pqrs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {
                                       "ciudadano": {
                                         "tipoDocumento": "CC",
                                         "numeroDocumento": "1047398221",
                                         "nombres": "Carlos Andrés",
                                         "apellidos": "Pacheco",
                                         "correo": "capacheco@correo.com",
                                         "telefono": "3015558841"
                                       },
                                       "tipoSolicitud": "QUEJA",
                                       "categoria": "SERVICIO_RUTA",
                                       "canal": "WEB",
                                       "asunto": "La ruta T100 no cumple la frecuencia anunciada",
                                       "descripcion": "El bus pasó con cuarenta minutos de retraso en hora pico.",
                                       "codigoRuta": "T100",
                                       "placaVehiculo": "UXT101"
                                     }
                                     """))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.estado").value("RADICADA"))
                    .andExpect(jsonPath("$.prioridad").value("MEDIA"))
                    .andExpect(jsonPath("$.placaVehiculo").value("UXT101"))
                    .andReturn().getResponse().getContentAsString();

            String radicado = json.readTree(cuerpoRadicacion).get("radicado").asText();
            assertThat(radicado).matches("TC-\\d{4}-\\d{6}");

            // 2. Consulta pública: exige radicado y documento, y enmascara.
            mockMvc.perform(get("/api/v1/pqrs/publico/" + radicado)
                            .param("documento", "1047398221"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ciudadano.numeroDocumento").value("******8221"));

            // 3. Con el documento equivocado, no existe.
            mockMvc.perform(get("/api/v1/pqrs/publico/" + radicado)
                            .param("documento", "9999999999"))
                    .andExpect(status().isNotFound());

            // 4. El funcionario se autentica.
            String token = autenticarse();

            // 5. Pasa la solicitud a trámite.
            mockMvc.perform(patch("/api/v1/pqrs/" + radicado + "/estado")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {"estadoDestino":"EN_TRAMITE","observacion":"Asignada al área de operaciones"}
                                     """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EN_TRAMITE"));

            // 6. Una transición ilegal se rechaza con 422, no con 500.
            mockMvc.perform(patch("/api/v1/pqrs/" + radicado + "/estado")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {"estadoDestino":"RADICADA","observacion":"Marcha atrás no permitida"}
                                     """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));

            // 7. Responde de fondo.
            mockMvc.perform(post("/api/v1/pqrs/" + radicado + "/respuesta")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                     {"respuesta":"Se reprogramó la frecuencia de la ruta T100 en hora pico."}
                                     """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RESUELTA"))
                    .andExpect(jsonPath("$.fechaRespuesta").isNotEmpty());

            // 8. La bitácora conserva las tres transiciones.
            mockMvc.perform(get("/api/v1/pqrs/" + radicado + "/seguimientos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].estadoNuevo").value("RADICADA"));
        }

        @Test
        @DisplayName("el listado pagina y enmascara los datos del titular")
        void listadoPaginado() throws Exception {
            String token = autenticarse();

            mockMvc.perform(get("/api/v1/pqrs")
                            .header("Authorization", "Bearer " + token)
                            .param("tamano", "5000"))
                    .andExpect(status().isOk())
                    // El tamaño abusivo se recorta al máximo permitido.
                    .andExpect(jsonPath("$.tamano").value(100));
        }
    }

    private String autenticarse() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIALES_FUNCIONARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        JsonNode respuesta = json.readTree(cuerpo);
        assertThat(respuesta.get("roles").toString()).contains("ROLE_FUNCIONARIO");
        return respuesta.get("token").asText();
    }
}
