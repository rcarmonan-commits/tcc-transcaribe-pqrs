package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Pqrs;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data del agregado PQRS.
 *
 * <p>Todas las consultas son JPQL con parámetros nombrados. No hay una sola
 * concatenación de cadenas en la construcción de las consultas, que es la
 * condición necesaria para que no exista inyección SQL (OWASP A03:2021). Los
 * filtros dinámicos se arman con la API de criterios en
 * {@code EspecificacionesPqrs}, no pegando texto.</p>
 */
public interface PqrsJpaRepository extends JpaRepository<Pqrs, Long>, JpaSpecificationExecutor<Pqrs> {

    Optional<Pqrs> findByRadicado(String radicado);

    boolean existsByRadicado(String radicado);

    long countByEstadoCodigo(String codigoEstado);

    long countByFechaRadicacionBetween(LocalDateTime desde, LocalDateTime hasta);

    long countByRespondidaEnPlazo(boolean enPlazo);

    /**
     * Solicitudes cuyo término venció y que siguen sin respuesta de fondo.
     * Se excluyen los estados finales para no reabrir lo ya archivado.
     */
    @Query("""
           SELECT p FROM Pqrs p
           WHERE p.fechaVencimiento < :fechaReferencia
             AND p.fechaRespuesta IS NULL
             AND p.estado.esFinal = false
             AND p.estado.codigo <> 'VENCIDA'
           ORDER BY p.fechaVencimiento ASC
           """)
    List<Pqrs> buscarVencidasSinRespuesta(@Param("fechaReferencia") LocalDate fechaReferencia);
}
