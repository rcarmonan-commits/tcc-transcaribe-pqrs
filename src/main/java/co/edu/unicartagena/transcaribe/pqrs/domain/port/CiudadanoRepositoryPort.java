package co.edu.unicartagena.transcaribe.pqrs.domain.port;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ciudadano;
import java.util.Optional;

public interface CiudadanoRepositoryPort {

    Ciudadano guardar(Ciudadano ciudadano);

    Optional<Ciudadano> buscarPorDocumento(String codigoTipoDocumento, String numeroDocumento);

    Optional<Ciudadano> buscarPorId(Long id);

    boolean existeDocumento(String codigoTipoDocumento, String numeroDocumento);
}
