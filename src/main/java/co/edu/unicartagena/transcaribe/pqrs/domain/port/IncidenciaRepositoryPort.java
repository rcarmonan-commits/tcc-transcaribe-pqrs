package co.edu.unicartagena.transcaribe.pqrs.domain.port;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Incidencia;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Pagina;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.consulta.Paginacion;
import java.util.Optional;

public interface IncidenciaRepositoryPort {

    Incidencia guardar(Incidencia incidencia);

    Optional<Incidencia> buscarPorConsecutivo(String consecutivo);

    Pagina<Incidencia> listarPorVehiculo(String placa, Paginacion paginacion);

    Pagina<Incidencia> listar(Paginacion paginacion);

    long contarPorTipo(String codigoTipo);

    long contarDelAnio(int anio);
}
