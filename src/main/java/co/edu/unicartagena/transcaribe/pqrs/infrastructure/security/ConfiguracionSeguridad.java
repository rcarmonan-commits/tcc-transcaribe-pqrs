package co.edu.unicartagena.transcaribe.pqrs.infrastructure.security;

import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de seguridad del servicio.
 *
 * <p>El criterio es denegar por omisión: todo lo que no esté explícitamente
 * abierto exige autenticación. Abrir rutas una por una obliga a justificar
 * cada excepción; hacerlo al revés, cerrando lo que se recuerda, deja huecos
 * en cuanto se añade un controlador nuevo.</p>
 *
 * <p>Controles aplicados y el riesgo que cierra cada uno:</p>
 * <table>
 *   <tr><td>Sesión sin estado</td><td>elimina la fijación de sesión</td></tr>
 *   <tr><td>CSRF desactivado</td><td>correcto <em>solo</em> porque no hay
 *       sesión ni cookies de autenticación: el testigo viaja en una cabecera
 *       que un formulario de un tercero no puede fijar</td></tr>
 *   <tr><td>CORS restrictivo</td><td>impide que un sitio cualquiera invoque el
 *       API con las credenciales del usuario</td></tr>
 *   <tr><td>Cabeceras de protección</td><td>reducen clickjacking, inferencia
 *       de tipo MIME y fuga por cabecera {@code Referer}</td></tr>
 *   <tr><td>BCrypt con coste 12</td><td>encarece el descifrado por fuerza
 *       bruta de la tabla de contraseñas si llegara a filtrarse</td></tr>
 * </table>
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    /** Factor de trabajo de BCrypt: más alto que el valor por defecto (10). */
    private static final int COSTE_BCRYPT = 12;

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/autenticacion/iniciar-sesion",
            "/api/v1/pqrs/publico/**",
            "/api/v1/catalogos/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    private final FiltroAutenticacionJwt filtroJwt;
    private final FiltroLimitePeticiones filtroLimite;
    private final ManejadorAccesoNoAutorizado manejadorRechazos;
    private final PropiedadesSeguridad propiedades;

    public ConfiguracionSeguridad(FiltroAutenticacionJwt filtroJwt,
                                  FiltroLimitePeticiones filtroLimite,
                                  ManejadorAccesoNoAutorizado manejadorRechazos,
                                  PropiedadesSeguridad propiedades) {
        this.filtroJwt = filtroJwt;
        this.filtroLimite = filtroLimite;
        this.manejadorRechazos = manejadorRechazos;
        this.propiedades = propiedades;
    }

    /**
     * Cadena propia para la documentación del API.
     *
     * <p>La política de contenido general es {@code default-src 'none'}, que es
     * la correcta para un servicio que solo devuelve JSON: el navegador no debe
     * ejecutar ni cargar nada. Pero Swagger UI <em>es</em> una página web, y con
     * esa política se queda en blanco: el navegador recibe su HTML y su
     * JavaScript con código 200 y se niega a ejecutarlos.</p>
     *
     * <p>Por eso la documentación lleva su propia política, limitada a sus
     * rutas: permite recursos del propio origen y los estilos y guiones en
     * línea que Swagger UI necesita para arrancar. El resto del servicio
     * conserva la política estricta. Y conviene recordar que en producción la
     * documentación está desactivada por omisión, así que esta relajación solo
     * existe donde se ha habilitado a propósito.</p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain cadenaDocumentacion(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(gestion -> gestion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
            .headers(cabeceras -> cabeceras
                    .frameOptions(marco -> marco.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; "
                                    + "script-src 'self' 'unsafe-inline'; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data:; "
                                    + "font-src 'self' data:; "
                                    + "connect-src 'self'; "
                                    + "frame-ancestors 'none'; base-uri 'none'")));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(gestion -> gestion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(cabeceras -> cabeceras
                    .frameOptions(marco -> marco.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .referrerPolicy(politica -> politica.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'")))
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers(RUTAS_PUBLICAS).permitAll()
                    // Radicar es un derecho de petición: no puede exigir cuenta.
                    .requestMatchers(HttpMethod.POST, "/api/v1/pqrs").permitAll()
                    // Las rutas activas son información de servicio al ciudadano.
                    .requestMatchers(HttpMethod.GET, "/api/v1/rutas").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .exceptionHandling(excepciones -> excepciones
                    // Sin credenciales -> 401; con credenciales insuficientes -> 403.
                    .authenticationEntryPoint(manejadorRechazos)
                    .accessDeniedHandler(manejadorRechazos))
            .addFilterBefore(filtroLimite, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.cors().origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setExposedHeaders(List.of("X-Rastreo"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(COSTE_BCRYPT);
    }
}
