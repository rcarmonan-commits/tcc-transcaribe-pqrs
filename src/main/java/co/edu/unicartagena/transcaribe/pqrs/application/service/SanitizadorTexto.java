package co.edu.unicartagena.transcaribe.pqrs.application.service;

import org.springframework.stereotype.Component;

/**
 * Normaliza el texto libre que llega del exterior.
 *
 * <p>Conviene ser explícito sobre qué hace y qué no hace esta clase, porque es
 * un punto donde se cometen errores con buena intención:</p>
 *
 * <ul>
 *   <li><b>Sí</b> elimina caracteres de control y normaliza espacios. Un salto
 *       de línea inyectado en un campo que después se escribe en una traza
 *       permite falsificar entradas de registro completas; es inyección de
 *       logs, dentro de A09:2021.</li>
 *   <li><b>No</b> escapa HTML. La defensa contra XSS corresponde a quien pinta
 *       el dato en el navegador, y hacerlo aquí guardaría en la base de datos
 *       un texto corrupto —{@code &amp;amp;} donde el ciudadano escribió
 *       {@code &amp;}— que además se re-escaparía en cada paso. El almacén
 *       guarda el dato real; cada consumidor lo codifica para su contexto.</li>
 * </ul>
 */
@Component
public class SanitizadorTexto {

    /** Caracteres de control salvo tabulación, salto de línea y retorno. */
    private static final String CONTROLES = "[\\p{Cntrl}&&[^\\t\\n\\r]]";

    /**
     * Limpia un texto destinado a persistirse: recorta, quita controles y
     * colapsa los espacios repetidos.
     *
     * @return el texto limpio, o {@code null} si la entrada era nula
     */
    public String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replaceAll(CONTROLES, "")
                .replaceAll("[ \\t]{2,}", " ")
                .strip();
    }

    /**
     * Prepara un valor para escribirlo en una traza: además de limpiarlo,
     * sustituye los saltos de línea para que un valor hostil no pueda fabricar
     * líneas de registro falsas.
     */
    public String paraRegistro(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = limpiar(texto).replaceAll("[\\r\\n]", "_");
        return limpio.length() > 200 ? limpio.substring(0, 200) + "…" : limpio;
    }

    /** Devuelve {@code null} cuando el texto viene vacío o en blanco. */
    public String nuloSiVacio(String texto) {
        String limpio = limpiar(texto);
        return (limpio == null || limpio.isBlank()) ? null : limpio;
    }
}
