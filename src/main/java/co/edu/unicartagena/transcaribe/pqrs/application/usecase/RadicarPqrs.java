package co.edu.unicartagena.transcaribe.pqrs.application.usecase;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.RadicarPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;

/**
 * Caso de uso: radicar una solicitud ciudadana.
 *
 * <p>Los casos de uso se declaran como interfaces de un solo método porque sus
 * clientes son distintos y tienen permisos distintos: radicar es público,
 * gestionar exige rol de funcionario y consultar indicadores exige auditoría.
 * Separarlos permite que cada controlador dependa exactamente de lo que
 * invoca, que es lo que pide el principio de segregación de interfaces, y que
 * una prueba sustituya uno sin arrastrar los demás.</p>
 */
public interface RadicarPqrs {

    PqrsRespuesta ejecutar(RadicarPqrsPeticion peticion);
}
