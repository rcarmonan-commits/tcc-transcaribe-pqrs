package co.edu.unicartagena.transcaribe.pqrs.application.usecase;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.CambiarEstadoPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.ResponderPqrsPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.PqrsRespuesta;

/**
 * Caso de uso: gestionar el ciclo de vida de una solicitud ya radicada.
 * Reservado a funcionarios autenticados.
 */
public interface GestionarPqrs {

    PqrsRespuesta cambiarEstado(String radicado, CambiarEstadoPeticion peticion, String nombreUsuario);

    PqrsRespuesta responder(String radicado, ResponderPqrsPeticion peticion, String nombreUsuario);

    PqrsRespuesta asignar(String radicado, String nombreUsuarioDestino, String nombreUsuarioQueAsigna);

    /**
     * Marca como vencidas las solicitudes cuyo término legal expiró.
     *
     * @return número de solicitudes afectadas
     */
    int marcarVencidas();
}
