package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.ConsecutivoRadicado;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio del contador anual de radicados. */
public interface ConsecutivoRadicadoJpaRepository extends JpaRepository<ConsecutivoRadicado, Integer> {

    /**
     * Toma la fila del año con bloqueo exclusivo.
     *
     * <p>Sin este bloqueo, dos radicaciones simultáneas leerían el mismo valor
     * y generarían el mismo número de radicado; la restricción de unicidad
     * salvaría la integridad, pero a costa de un error en la cara del
     * ciudadano. El bloqueo pesimista serializa únicamente esta fila, que se
     * retiene durante microsegundos.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConsecutivoRadicado c WHERE c.anio = :anio")
    Optional<ConsecutivoRadicado> bloquearPorAnio(@Param("anio") Integer anio);
}
