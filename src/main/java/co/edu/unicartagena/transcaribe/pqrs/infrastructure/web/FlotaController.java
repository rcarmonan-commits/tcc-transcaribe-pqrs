package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearRutaPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CrearVehiculoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PaginaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.RutaRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.VehiculoRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.service.GestionFlotaService;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administración de la flota y de las rutas del sistema. */
@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Flota", description = "Vehículos y rutas del sistema integrado de transporte")
public class FlotaController {

    private final GestionFlotaService servicio;

    public FlotaController(GestionFlotaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/vehiculos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vincula un vehículo a la flota")
    public ResponseEntity<VehiculoRespuesta> registrarVehiculo(
            @Valid @RequestBody CrearVehiculoPeticion peticion) {

        VehiculoRespuesta creado = servicio.registrarVehiculo(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/vehiculos/" + creado.placa()))
                .body(creado);
    }

    @GetMapping("/vehiculos/{placa}")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Consulta un vehículo por placa")
    public ResponseEntity<VehiculoRespuesta> consultarVehiculo(
            @PathVariable @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$") String placa) {

        return ResponseEntity.ok(servicio.consultarVehiculo(placa));
    }

    @GetMapping("/vehiculos")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'AUDITOR')")
    @Operation(summary = "Listado paginado de la flota")
    public ResponseEntity<PaginaRespuesta<VehiculoRespuesta>> listarVehiculos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "placa") String ordenarPor,
            @RequestParam(defaultValue = "false") boolean descendente) {

        return ResponseEntity.ok(
                servicio.listarVehiculos(new Paginacion(pagina, tamano, ordenarPor, descendente)));
    }

    @PostMapping("/rutas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra una ruta del sistema")
    public ResponseEntity<RutaRespuesta> registrarRuta(@Valid @RequestBody CrearRutaPeticion peticion) {
        RutaRespuesta creada = servicio.registrarRuta(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/rutas/" + creada.codigo()))
                .body(creada);
    }

    @GetMapping("/rutas")
    @Operation(summary = "Rutas activas. Acceso público: es información de servicio al ciudadano.")
    public ResponseEntity<List<RutaRespuesta>> listarRutas() {
        return ResponseEntity.ok(servicio.listarRutasActivas());
    }
}
