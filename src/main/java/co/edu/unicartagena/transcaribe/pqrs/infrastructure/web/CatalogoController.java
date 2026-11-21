package co.edu.unicartagena.transcaribe.pqrs.infrastructure.web;

import co.edu.unicartagena.transcaribe.pqrs.domain.port.CatalogoRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogos que necesita el formulario público de radicación.
 *
 * <p>Es acceso anónimo porque el ciudadano debe poder elegir el tipo de
 * solicitud y la categoría antes de identificarse. No se expone ningún dato
 * sensible: son listas de códigos y nombres. El plazo legal de cada tipo
 * también se publica, y eso es deseable: el ciudadano tiene derecho a saber
 * en cuántos días hábiles debe recibir respuesta.</p>
 */
@RestController
@RequestMapping("/api/v1/catalogos")
@Tag(name = "Catálogos", description = "Listas de referencia para el formulario de radicación")
public class CatalogoController {

    private final CatalogoRepositoryPort catalogo;

    public CatalogoController(CatalogoRepositoryPort catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/tipos-solicitud")
    @Operation(summary = "Tipos de solicitud con su plazo legal de respuesta")
    public ResponseEntity<List<Map<String, Object>>> tiposSolicitud() {
        return ResponseEntity.ok(catalogo.listarTiposSolicitud().stream()
                .map(tipo -> Map.<String, Object>of(
                        "codigo", tipo.getCodigo(),
                        "nombre", tipo.getNombre(),
                        "diasHabilesRespuesta", tipo.getDiasHabilesRespuesta(),
                        "fundamentoLegal", tipo.getFundamentoLegal()))
                .toList());
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorías temáticas activas")
    public ResponseEntity<List<Map<String, Object>>> categorias() {
        return ResponseEntity.ok(catalogo.listarCategoriasActivas().stream()
                .map(categoria -> Map.<String, Object>of(
                        "codigo", categoria.getCodigo(),
                        "nombre", categoria.getNombre()))
                .toList());
    }

    @GetMapping("/canales")
    @Operation(summary = "Canales de recepción habilitados")
    public ResponseEntity<List<Map<String, Object>>> canales() {
        return ResponseEntity.ok(catalogo.listarCanales().stream()
                .map(canal -> Map.<String, Object>of(
                        "codigo", canal.getCodigo(),
                        "nombre", canal.getNombre()))
                .toList());
    }
}
