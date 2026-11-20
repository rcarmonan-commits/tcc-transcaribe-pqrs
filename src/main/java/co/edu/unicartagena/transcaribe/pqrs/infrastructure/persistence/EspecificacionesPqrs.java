package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.FiltroPqrs;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Construye la consulta dinámica de solicitudes con la API de criterios.
 *
 * <p>Esta es la alternativa correcta a concatenar un {@code WHERE} con las
 * cadenas que llegan del cliente. Cada criterio se añade como predicado
 * tipado y los valores viajan como parámetros vinculados, de modo que un
 * contenido hostil en un filtro nunca se interpreta como SQL
 * (OWASP A03:2021, Inyección).</p>
 */
public final class EspecificacionesPqrs {

    private EspecificacionesPqrs() {
        // Clase de utilidad.
    }

    public static Specification<Pqrs> desde(FiltroPqrs filtro, LocalDate fechaReferencia) {
        return (raiz, consulta, constructor) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (filtro.estado() != null) {
                predicados.add(constructor.equal(
                        raiz.get("estado").get("codigo"), filtro.estado().name()));
            }
            if (filtro.prioridad() != null) {
                predicados.add(constructor.equal(raiz.get("prioridad"), filtro.prioridad()));
            }
            if (textoPresente(filtro.codigoTipoSolicitud())) {
                predicados.add(constructor.equal(
                        raiz.get("tipoSolicitud").get("codigo"), filtro.codigoTipoSolicitud()));
            }
            if (textoPresente(filtro.codigoCategoria())) {
                predicados.add(constructor.equal(
                        raiz.get("categoria").get("codigo"), filtro.codigoCategoria()));
            }
            if (textoPresente(filtro.numeroDocumentoCiudadano())) {
                predicados.add(constructor.equal(
                        raiz.get("ciudadano").get("numeroDocumento"), filtro.numeroDocumentoCiudadano()));
            }
            if (textoPresente(filtro.codigoRuta())) {
                predicados.add(constructor.equal(
                        raiz.get("ruta").get("codigo"), filtro.codigoRuta()));
            }
            if (textoPresente(filtro.placaVehiculo())) {
                predicados.add(constructor.equal(
                        raiz.get("vehiculo").get("placa"), filtro.placaVehiculo().toUpperCase()));
            }
            if (filtro.desde() != null) {
                predicados.add(constructor.greaterThanOrEqualTo(
                        raiz.get("fechaRadicacion"), filtro.desde().atStartOfDay()));
            }
            if (filtro.hasta() != null) {
                predicados.add(constructor.lessThanOrEqualTo(
                        raiz.get("fechaRadicacion"), filtro.hasta().atTime(LocalTime.MAX)));
            }
            if (filtro.pideSoloVencidas()) {
                predicados.add(constructor.lessThan(raiz.get("fechaVencimiento"), fechaReferencia));
                predicados.add(constructor.isNull(raiz.get("fechaRespuesta")));
            }

            return constructor.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static boolean textoPresente(String valor) {
        return valor != null && !valor.isBlank();
    }
}
