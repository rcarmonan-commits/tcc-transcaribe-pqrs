package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del servicio.
 *
 * <p>La interfaz de Swagger queda desactivada en el perfil de producción
 * ({@code springdoc.swagger-ui.enabled=false}): un catálogo completo de
 * operaciones y esquemas publicado sin necesidad es superficie de ataque
 * regalada.</p>
 */
@Configuration
public class ConfiguracionOpenApi {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    OpenAPI documentacionApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de PQRS e incidencias de flota — Transcaribe S.A.")
                        .version("1.0.0")
                        .description("""
                                Servicio REST para la radicación, gestión y trazabilidad de
                                peticiones, quejas, reclamos y sugerencias del Sistema Integrado
                                de Transporte Masivo de Cartagena, y para el registro de
                                incidencias operativas de la flota.

                                Los plazos de respuesta se calculan en días hábiles conforme al
                                artículo 14 de la Ley 1755 de 2015.
                                """)
                        .contact(new Contact()
                                .name("Universidad de Cartagena — Ingeniería de Sistemas"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Testigo obtenido en /api/v1/autenticacion/iniciar-sesion")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
