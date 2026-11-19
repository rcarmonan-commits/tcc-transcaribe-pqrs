package co.edu.unicartagena.transcaribe.pqrs.domain.port;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Ruta;
import java.util.List;
import java.util.Optional;

public interface RutaRepositoryPort {

    Ruta guardar(Ruta ruta);

    Optional<Ruta> buscarPorCodigo(String codigo);

    boolean existeCodigo(String codigo);

    List<Ruta> listarActivas();
}
