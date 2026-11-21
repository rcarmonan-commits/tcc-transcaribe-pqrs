package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.ErrorRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.OperacionNoPermitidaException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoDuplicadoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.RecursoNoEncontradoException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.ReglaNegocioException;
import co.edu.unicartagena.transcaribe.pqrs.domain.exception.TransicionInvalidaException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce excepciones a respuestas HTTP.
 *
 * <p>Su responsabilidad real es contener la fuga de información. Cada
 * manejador decide dos cosas por separado: <em>qué se registra</em> —con todo
 * el detalle, del lado del servidor— y <em>qué se devuelve</em>, que es siempre
 * un mensaje acotado. El identificador de rastreo une ambas caras: el usuario
 * puede citarlo en un reporte y el operador encuentra la traza completa sin que
 * el mensaje haya revelado nada (OWASP A05:2021 y A09:2021).</p>
 *
 * <p>El caso más delicado es {@link DataIntegrityViolationException}: su
 * mensaje contiene nombres de tablas, índices y a veces el valor que falló.
 * Nunca sale al cliente.</p>
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e) {
        String rastreo = nuevoRastreo();
        LOG.info("[{}] Recurso no encontrado: {}", rastreo, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorRespuesta.de(e.getCodigo(), e.getMessage(), rastreo));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> duplicado(RecursoDuplicadoException e) {
        String rastreo = nuevoRastreo();
        LOG.info("[{}] Recurso duplicado: {}", rastreo, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorRespuesta.de(e.getCodigo(), e.getMessage(), rastreo));
    }

    @ExceptionHandler({TransicionInvalidaException.class, OperacionNoPermitidaException.class})
    public ResponseEntity<ErrorRespuesta> reglaDeNegocio(ReglaNegocioException e) {
        String rastreo = nuevoRastreo();
        LOG.info("[{}] Regla de negocio: {}", rastreo, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorRespuesta.de(e.getCodigo(), e.getMessage(), rastreo));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(ErrorRespuesta.deValidacion(detalles, nuevoRastreo()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> violacionDeRestriccion(ConstraintViolationException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(violacion ->
                detalles.putIfAbsent(violacion.getPropertyPath().toString(), violacion.getMessage()));

        return ResponseEntity.badRequest()
                .body(ErrorRespuesta.deValidacion(detalles, nuevoRastreo()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> peticionMalFormada(Exception e) {
        String rastreo = nuevoRastreo();
        LOG.info("[{}] Petición mal formada: {}", rastreo, e.getClass().getSimpleName());
        return ResponseEntity.badRequest().body(ErrorRespuesta.de(
                "PETICION_MAL_FORMADA",
                "El cuerpo o los parámetros de la petición no tienen el formato esperado",
                rastreo));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorRespuesta> credenciales(BadCredentialsException e) {
        // No se registra el nombre de usuario aquí: ya quedó en la auditoría,
        // que es el sitio previsto para ese dato.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorRespuesta.de(
                "CREDENCIALES_INVALIDAS", "Credenciales inválidas", nuevoRastreo()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException e) {
        String rastreo = nuevoRastreo();
        LOG.warn("[{}] Acceso denegado", rastreo);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorRespuesta.de(
                "ACCESO_DENEGADO", "No tiene permisos para ejecutar esta operación", rastreo));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        String rastreo = nuevoRastreo();
        // El detalle, que incluye nombres de índices y valores, solo al registro.
        LOG.error("[{}] Violación de integridad en la base de datos", rastreo, e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorRespuesta.de(
                "CONFLICTO_DE_DATOS",
                "La operación entra en conflicto con información ya registrada",
                rastreo));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> noControlada(Exception e) {
        String rastreo = nuevoRastreo();
        LOG.error("[{}] Error no controlado", rastreo, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorRespuesta.de(
                "ERROR_INTERNO",
                "Ocurrió un error inesperado. Cite el código de rastreo al reportarlo.",
                rastreo));
    }

    private String nuevoRastreo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
