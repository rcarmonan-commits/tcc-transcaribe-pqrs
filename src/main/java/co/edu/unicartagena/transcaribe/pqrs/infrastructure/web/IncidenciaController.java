package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RegistrarIncidenciaPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.IncidenciaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.service.GestionIncidenciasService;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Incidencias operativas de la flota.
 *
 * <p>Todo el recurso exige autenticación: una incidencia describe un fallo
 * interno de la operación y publicarla sin control daría a cualquiera el mapa
 * del estado real de la flota.</p>
 */
@RestController
@RequestMapping("/api/v1/incidencias")
@Validated
@Tag(name = "Incidencias", description = "Registro y seguimiento de eventos operativos de la flota")
public class IncidenciaController {

    private static final String PATRON_CONSECUTIVO = "^INC-[0-9]{4}-[0-9]{5}$";

    private final GestionIncidenciasService servicio;

    public IncidenciaController(GestionIncidenciasService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    @Operation(summary = "Registra una incidencia de flota")
    public ResponseEntity<IncidenciaRespuesta> registrar(
            @Valid @RequestBody RegistrarIncidenciaPeticion peticion,
            Principal autenticado) {

        IncidenciaRespuesta creada = servicio.registrar(peticion, autenticado.getName());
        return ResponseEntity
                .created(URI.create("/api/v1/incidencias/" + creada.consecutivo()))
                .body(creada);
    }

    @PatchMapping("/{consecutivo}/atencion")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    @Operation(summary = "Toma la incidencia en atención")
    public ResponseEntity<IncidenciaRespuesta> tomarEnAtencion(
            @PathVariable @Pattern(regexp = PATRON_CONSECUTIVO) String consecutivo) {

        return ResponseEntity.ok(servicio.tomarEnAtencion(consecutivo));
    }

    @PatchMapping("/{consecutivo}/cierre")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    @Operation(summary = "Cierra la incidencia")
    public ResponseEntity<IncidenciaRespuesta> cerrar(
            @PathVariable @Pattern(regexp = PATRON_CONSECUTIVO) String consecutivo) {

        return ResponseEntity.ok(servicio.cerrar(consecutivo));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Listado paginado de incidencias")
    public ResponseEntity<PaginaRespuesta<IncidenciaRespuesta>> listar(
            @RequestParam(required = false) String placa,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "fechaHora") String ordenarPor,
            @RequestParam(defaultValue = "true") boolean descendente) {

        Paginacion paginacion = new Paginacion(pagina, tamano, ordenarPor, descendente);

        return ResponseEntity.ok(
                (placa == null || placa.isBlank())
                        ? servicio.listar(paginacion)
                        : servicio.listarPorVehiculo(placa, paginacion));
    }
}
