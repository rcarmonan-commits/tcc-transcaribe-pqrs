package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de rutas del sistema integrado. */
public interface RutaJpaRepository extends JpaRepository<Ruta, Integer> {

    Optional<Ruta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Ruta> findByActivaTrueOrderByCodigoAsc();
}
