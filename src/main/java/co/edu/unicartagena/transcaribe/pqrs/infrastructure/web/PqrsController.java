package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CambiarEstadoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RadicarPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.ResponderPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsResumenRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.SeguimientoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.ConsultarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.GestionarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.application.usecase.RadicarPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.CodigoEstadoPqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.enums.Prioridad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Recursos de solicitudes ciudadanas.
 *
 * <p>El controlador no contiene una sola regla de negocio: valida la forma de
 * la petición, resuelve quién la hace y delega. Depende de las tres interfaces
 * de caso de uso por separado, no de un servicio único con doce métodos, de
 * modo que cada operación declara exactamente de qué depende.</p>
 *
 * <p>La autorización se declara por método con {@code @PreAuthorize}. Es más
 * verboso que una regla global por ruta, pero deja el permiso escrito al lado
 * de la operación que protege, donde se ve al leer el código.</p>
 */
@RestController
@RequestMapping("/api/v1/pqrs")
@Validated
@Tag(name = "PQRS", description = "Radicación, consulta y gestión de solicitudes ciudadanas")
public class PqrsController {

    private static final String PATRON_RADICADO = "^[A-Z]{2,5}-[0-9]{4}-[0-9]{6}$";

    private final RadicarPqrs radicarPqrs;
    private final ConsultarPqrs consultarPqrs;
    private final GestionarPqrs gestionarPqrs;

    public PqrsController(RadicarPqrs radicarPqrs,
                          ConsultarPqrs consultarPqrs,
                          GestionarPqrs gestionarPqrs) {
        this.radicarPqrs = radicarPqrs;
        this.consultarPqrs = consultarPqrs;
        this.gestionarPqrs = gestionarPqrs;
    }

    @PostMapping
    @Operation(summary = "Radica una solicitud ciudadana. Acceso público.")
    public ResponseEntity<PqrsRespuesta> radicar(@Valid @RequestBody RadicarPqrsPeticion peticion) {
        PqrsRespuesta creada = radicarPqrs.ejecutar(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/pqrs/" + creada.radicado()))
                .body(creada);
    }

    @GetMapping("/publico/{radicado}")
    @Operation(summary = "Consulta ciudadana: exige radicado y documento del titular")
    public ResponseEntity<PqrsRespuesta> consultaCiudadana(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado,
            @RequestParam @Pattern(regexp = "^[0-9]{5,20}$") String documento) {

        return ResponseEntity.ok(consultarPqrs.consultaCiudadana(radicado, documento));
    }

    @GetMapping("/{radicado}")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Detalle completo de una solicitud")
    public ResponseEntity<PqrsRespuesta> detalle(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado) {

        return ResponseEntity.ok(consultarPqrs.porRadicado(radicado));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Listado paginado de solicitudes con filtros")
    public ResponseEntity<PaginaRespuesta<PqrsResumenRespuesta>> buscar(
            @RequestParam(required = false) CodigoEstadoPqrs estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) String tipoSolicitud,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String ruta,
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean vencidas,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "fechaRadicacion") String ordenarPor,
            @RequestParam(defaultValue = "true") boolean descendente) {

        FiltroPqrs filtro = new FiltroPqrs(estado, prioridad, tipoSolicitud, categoria,
                documento, ruta, placa, desde, hasta, vencidas);

        return ResponseEntity.ok(
                consultarPqrs.buscar(filtro, new Paginacion(pagina, tamano, ordenarPor, descendente)));
    }

    @GetMapping("/{radicado}/seguimientos")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Bitácora de transiciones de una solicitud")
    public ResponseEntity<List<SeguimientoRespuesta>> seguimientos(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado) {

        return ResponseEntity.ok(consultarPqrs.trazabilidad(radicado));
    }

    @PatchMapping("/{radicado}/estado")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    @Operation(summary = "Cambia el estado de una solicitud")
    public ResponseEntity<PqrsRespuesta> cambiarEstado(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado,
            @Valid @RequestBody CambiarEstadoPeticion peticion,
            Principal autenticado) {

        return ResponseEntity.ok(
                gestionarPqrs.cambiarEstado(radicado, peticion, autenticado.getName()));
    }

    @PostMapping("/{radicado}/respuesta")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    @Operation(summary = "Registra la respuesta de fondo y deja la solicitud resuelta")
    public ResponseEntity<PqrsRespuesta> responder(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado,
            @Valid @RequestBody ResponderPqrsPeticion peticion,
            Principal autenticado) {

        return ResponseEntity.ok(
                gestionarPqrs.responder(radicado, peticion, autenticado.getName()));
    }

    @PostMapping("/{radicado}/asignacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Asigna la solicitud a un funcionario")
    public ResponseEntity<PqrsRespuesta> asignar(
            @PathVariable @Pattern(regexp = PATRON_RADICADO) String radicado,
            @RequestParam String funcionario,
            Principal autenticado) {

        return ResponseEntity.ok(
                gestionarPqrs.asignar(radicado, funcionario, autenticado.getName()));
    }
}
