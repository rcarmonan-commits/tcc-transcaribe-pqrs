package co.edu.unicartagena.transcaribe.pqrs.domain.port;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Vehiculo;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import java.util.Optional;

public interface VehiculoRepositoryPort {

    Vehiculo guardar(Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorPlaca(String placa);

    boolean existePlaca(String placa);

    boolean existeNumeroInterno(String numeroInterno);

    Pagina<Vehiculo> listar(Paginacion paginacion);
}
