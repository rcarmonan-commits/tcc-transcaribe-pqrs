package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.AutenticacionPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.AutenticacionRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Punto de entrada de autenticación de funcionarios. */
@RestController
@RequestMapping("/api/v1/autenticacion")
@Tag(name = "Autenticación", description = "Obtención del testigo de acceso para funcionarios")
public class AutenticacionController {

    private final AutenticacionService servicio;

    public AutenticacionController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/iniciar-sesion")
    @Operation(summary = "Inicia sesión y devuelve un testigo JWT de vigencia limitada")
    public ResponseEntity<AutenticacionRespuesta> iniciarSesion(
            @Valid @RequestBody AutenticacionPeticion peticion,
            HttpServletRequest http) {

        return ResponseEntity.ok(servicio.iniciarSesion(peticion, http.getRemoteAddr()));
    }
}
