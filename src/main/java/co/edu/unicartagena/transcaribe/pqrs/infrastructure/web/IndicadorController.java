package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.IndicadoresRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.application.service.IndicadoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores de gestión del servicio de PQRS. */
@RestController
@RequestMapping("/api/v1/indicadores")
@Tag(name = "Indicadores", description = "Cifras de oportunidad y estado del servicio de PQRS")
public class IndicadorController {

    private final IndicadoresService servicio;

    public IndicadorController(IndicadoresService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Operation(summary = "Indicadores del periodo; por omisión, el año en curso")
    public ResponseEntity<IndicadoresRespuesta> indicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(servicio.calcular(desde, hasta));
    }
}
