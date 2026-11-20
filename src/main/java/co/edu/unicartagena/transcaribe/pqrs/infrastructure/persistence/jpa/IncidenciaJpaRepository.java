package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de incidencias de flota. */
public interface IncidenciaJpaRepository extends JpaRepository<Incidencia, Long> {

    Optional<Incidencia> findByConsecutivo(String consecutivo);

    Page<Incidencia> findByVehiculoPlaca(String placa, Pageable pageable);

    long countByTipoIncidenciaCodigo(String codigoTipo);

    long countByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);
}
