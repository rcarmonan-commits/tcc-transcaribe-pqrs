package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.AuditoriaAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data del registro de eventos de seguridad. */
public interface AuditoriaAccesoJpaRepository extends JpaRepository<AuditoriaAcceso, Long> {
}
