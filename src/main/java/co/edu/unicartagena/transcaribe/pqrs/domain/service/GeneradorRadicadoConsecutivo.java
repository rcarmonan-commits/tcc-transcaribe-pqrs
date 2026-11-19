package co.edu.unicartagena.transcaribe.pqrs.domain.service;

import co.edu.unicartagena.transcaribe.pqrs.domain.port.ConsecutivoRadicadoPort;
import java.util.Objects;

/**
 * Genera radicados con el formato {@code PREFIJO-AAAA-NNNNNN}, por ejemplo
 * {@code TC-2026-000147}.
 *
 * <p>Regla de negocio RN-02: el radicado es único, consecutivo dentro del año
 * y no reutilizable. Es el único identificador que se le entrega al ciudadano,
 * y por eso no expone la clave primaria interna: publicar el {@code id} de la
 * tabla permitiría enumerar solicitudes ajenas, que es exactamente el patrón
 * de referencia directa insegura de OWASP A01:2021.</p>
 */
public class GeneradorRadicadoConsecutivo implements GeneradorRadicado {

    private static final int DIGITOS_CONSECUTIVO = 6;

    private final ConsecutivoRadicadoPort consecutivos;
    private final String prefijo;

    public GeneradorRadicadoConsecutivo(ConsecutivoRadicadoPort consecutivos, String prefijo) {
        this.consecutivos = Objects.requireNonNull(consecutivos, "consecutivos");
        this.prefijo = Objects.requireNonNull(prefijo, "prefijo");
    }

    @Override
    public String generar(int anio) {
        long consecutivo = consecutivos.siguienteConsecutivo(anio);
        String plantilla = "%s-%d-%0" + DIGITOS_CONSECUTIVO + "d";
        return String.format(plantilla, prefijo, anio, consecutivo);
    }
}
