package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de funcionarios del sistema. */
public interface UsuarioJpaRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}
