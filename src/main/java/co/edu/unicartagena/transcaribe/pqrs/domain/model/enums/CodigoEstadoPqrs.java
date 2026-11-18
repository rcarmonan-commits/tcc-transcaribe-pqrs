package co.edu.unicartagena.transcaribe.pqrs.domain.model.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Códigos de estado de una PQRS y máquina de transiciones permitidas.
 *
 * <p>La tabla {@code estado_pqrs} guarda el catálogo (nombre legible, orden,
 * marca de estado final); este tipo guarda la <em>regla</em>. Separarlos evita
 * que una fila mal editada en la base de datos abra una transición ilegal.</p>
 *
 * <p>Regla de negocio RN-04: toda transición que no esté en este grafo se
 * rechaza, incluida la transición de un estado a sí mismo.</p>
 *
 * <pre>
 *   RADICADA ──► EN_TRAMITE ──► RESUELTA ──► CERRADA
 *       │             │             │
 *       └──► VENCIDA ◄┘             │
 *                │                  │
 *                └──► EN_TRAMITE ───┘
 * </pre>
 */
public enum CodigoEstadoPqrs {

    RADICADA,
    EN_TRAMITE,
    RESUELTA,
    CERRADA,
    VENCIDA;

    private static final Map<CodigoEstadoPqrs, Set<CodigoEstadoPqrs>> TRANSICIONES = Map.of(
            RADICADA,   EnumSet.of(EN_TRAMITE, VENCIDA),
            EN_TRAMITE, EnumSet.of(RESUELTA, VENCIDA),
            RESUELTA,   EnumSet.of(CERRADA, EN_TRAMITE),
            VENCIDA,    EnumSet.of(EN_TRAMITE, RESUELTA),
            CERRADA,    EnumSet.noneOf(CodigoEstadoPqrs.class)
    );

    /** @return {@code true} si el paso de este estado al destino está permitido. */
    public boolean permiteTransicionA(CodigoEstadoPqrs destino) {
        if (destino == null) {
            return false;
        }
        return TRANSICIONES.get(this).contains(destino);
    }

    /** Estados a los que se puede llegar desde este. Conjunto inmutable. */
    public Set<CodigoEstadoPqrs> transicionesPermitidas() {
        return EnumSet.copyOf(TRANSICIONES.get(this));
    }

    /** Un estado final no admite ninguna salida: la solicitud quedó archivada. */
    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }

    /** Solo en estos estados tiene sentido registrar una respuesta de fondo. */
    public boolean admiteRespuesta() {
        return this == EN_TRAMITE || this == VENCIDA;
    }
}
