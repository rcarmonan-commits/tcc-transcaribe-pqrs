package co.edu.unicartagena.transcaribe.pqrs.infrastructure.security;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde a los rechazos de la cadena de seguridad con el código correcto y
 * el mismo cuerpo de error que usa el resto del API.
 *
 * <p>Sin esta pieza, Spring Security responde 403 a una petición <em>sin</em>
 * credenciales, que es semánticamente incorrecto: 403 significa "te conozco y
 * aun así no puedes", mientras que lo que ocurrió es que nadie se identificó.
 * La distinción no es cosmética — un cliente que recibe 401 sabe que debe
 * autenticarse y reintentar; uno que recibe 403 concluye que no tiene permisos
 * y abandona.</p>
 *
 * <p>La respuesta tampoco puede ser la página de error por defecto del
 * contenedor: delata la pila tecnológica y rompe el contrato JSON.</p>
 */
@Component
public class ManejadorAccesoNoAutorizado implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorAccesoNoAutorizado.class);

    private final ObjectMapper json;

    public ManejadorAccesoNoAutorizado(ObjectMapper json) {
        this.json = json;
    }

    /** Falta la credencial o no es válida: 401. */
    @Override
    public void commence(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {

        escribir(respuesta, HttpStatus.UNAUTHORIZED,
                "NO_AUTENTICADO",
                "Se requiere un testigo de acceso válido para esta operación");
    }

    /** La credencial es válida pero el rol no alcanza: 403. */
    @Override
    public void handle(HttpServletRequest peticion,
                       HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {

        escribir(respuesta, HttpStatus.FORBIDDEN,
                "ACCESO_DENEGADO",
                "No tiene permisos para ejecutar esta operación");
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String codigo, String mensaje)
            throws IOException {

        String rastreo = UUID.randomUUID().toString().substring(0, 8);
        LOG.debug("[{}] Rechazo de la cadena de seguridad: {}", rastreo, codigo);

        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), ErrorRespuesta.de(codigo, mensaje, rastreo));
    }
}
