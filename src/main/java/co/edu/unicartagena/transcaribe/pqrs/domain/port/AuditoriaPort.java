package co.edu.unicartagena.transcaribe.pqrs.domain.port;

/**
 * Registro de eventos de seguridad (OWASP A09:2021).
 *
 * <p>Se declara como puerto para que el caso de uso pueda auditar sin saber si
 * el destino final es la base de datos, un archivo o un agregador externo.</p>
 */
public interface AuditoriaPort {

    void registrar(String nombreUsuario, String accion, String recurso, String ipOrigen, boolean exitoso);
}
