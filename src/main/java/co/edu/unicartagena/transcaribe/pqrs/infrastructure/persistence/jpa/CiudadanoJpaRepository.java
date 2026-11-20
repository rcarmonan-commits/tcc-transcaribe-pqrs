package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de ciudadanos. */
public interface CiudadanoJpaRepository extends JpaRepository<Ciudadano, Long> {

    Optional<Ciudadano> findByTipoDocumentoCodigoAndNumeroDocumento(String codigoTipoDocumento,
                                                                    String numeroDocumento);

    boolean existsByTipoDocumentoCodigoAndNumeroDocumento(String codigoTipoDocumento,
                                                          String numeroDocumento);
}
