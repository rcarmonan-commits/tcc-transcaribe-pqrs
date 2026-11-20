package co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.AuditoriaAcceso;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.AuditoriaPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.AuditoriaAccesoJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste los eventos de seguridad (OWASP A09:2021).
 *
 * <p>Dos decisiones merecen explicación:</p>
 *
 * <ul>
 *   <li><b>Transacción propia.</b> El registro se escribe en una transacción
 *       nueva, de modo que un intento de acceso fallido quede grabado aunque la
 *       operación que lo provocó termine en excepción y revierta. Una auditoría
 *       que desaparece con el error que debía documentar no sirve de nada.</li>
 *   <li><b>IP resumida con SHA-256 y sal.</b> Permite agrupar intentos del
 *       mismo origen sin almacenar un dato personal en claro, conforme al
 *       principio de minimización de la Ley 1581 de 2012.</li>
 * </ul>
 *
 * <p>Si la escritura del registro falla, se deja constancia en la traza pero no
 * se propaga la excepción: la auditoría no debe tumbar la operación que audita.</p>
 */
@Repository
public class AuditoriaAdapter implements AuditoriaPort {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaAdapter.class);
    private static final String SAL = "transcaribe-pqrs-auditoria";

    private final AuditoriaAccesoJpaRepository repositorio;
    private final Reloj reloj;

    public AuditoriaAdapter(AuditoriaAccesoJpaRepository repositorio, Reloj reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String nombreUsuario, String accion, String recurso, String ipOrigen, boolean exitoso) {
        try {
            repositorio.save(new AuditoriaAcceso(
                    recortar(nombreUsuario, 60),
                    recortar(accion, 40),
                    recortar(recurso, 200),
                    resumir(ipOrigen),
                    exitoso,
                    reloj.ahora()));
        } catch (RuntimeException e) {
            LOG.error("No se pudo registrar el evento de auditoría: accion={} exitoso={}", accion, exitoso, e);
        }
    }

    private String resumir(String ipOrigen) {
        String valor = (ipOrigen == null || ipOrigen.isBlank()) ? "desconocida" : ipOrigen;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resumen = digest.digest((SAL + valor).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda implementación de la plataforma.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return "desconocido";
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
