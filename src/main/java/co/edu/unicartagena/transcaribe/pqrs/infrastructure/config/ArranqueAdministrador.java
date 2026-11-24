package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import co.edu.unicartagena.transcaribe.pqrs.domain.model.Rol;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.RolJpaRepository;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.persistence.jpa.UsuarioJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea la primera cuenta de administración cuando la base no tiene ninguna.
 *
 * <p>El perfil de producción no carga los datos de demostración, y eso es
 * deliberado: mantener cuentas de contraseña conocida fuera del camino de
 * migración de producción cierra el riesgo de desplegarlas por descuido. Pero
 * tiene una consecuencia incómoda: un despliegue recién hecho no admite a
 * nadie, de modo que no se puede ni asignar una solicitud ni consultar los
 * indicadores.</p>
 *
 * <p>Esta clase resuelve ese arranque en frío de la forma habitual en un
 * servicio real: la primera cuenta se toma del entorno, no del repositorio.
 * Se crea <em>solo</em> si la tabla de usuarios está vacía, de modo que un
 * reinicio no sobrescribe nada ni reabre una cuenta que alguien desactivó.</p>
 *
 * <p>Si las variables no están definidas, el servicio arranca igual y lo dice
 * en el registro. No falla: las rutas públicas —radicar y consultar— son un
 * derecho de petición y deben seguir atendiéndose aunque nadie administre
 * todavía.</p>
 *
 * <pre>
 *   ADMIN_USUARIO=rcarmonan
 *   ADMIN_CORREO=rcarmonan@unicartagena.edu.co
 *   ADMIN_CONTRASENA=&lt;al menos 12 caracteres&gt;
 * </pre>
 */
@Component
public class ArranqueAdministrador implements ApplicationRunner {

    /** Longitud mínima de la contraseña inicial. */
    static final int LONGITUD_MINIMA = 12;

    private static final Logger LOG = LoggerFactory.getLogger(ArranqueAdministrador.class);

    /** Qué hacer al arrancar, resuelto sin tocar la base ni el entorno. */
    enum Decision {
        /** Ya hay cuentas: no se toca nada. */
        YA_HAY_CUENTAS,
        /** No se configuró ninguna cuenta inicial. */
        SIN_CONFIGURAR,
        /** La contraseña propuesta es demasiado corta para una cuenta con todos los permisos. */
        CONTRASENA_CORTA,
        /** Procede crearla. */
        CREAR
    }

    private final UsuarioJpaRepository usuarios;
    private final RolJpaRepository roles;
    private final PasswordEncoder codificador;
    private final String usuarioInicial;
    private final String correoInicial;
    private final String contrasenaInicial;

    public ArranqueAdministrador(UsuarioJpaRepository usuarios,
                                 RolJpaRepository roles,
                                 PasswordEncoder codificador,
                                 @Value("${ADMIN_USUARIO:}") String usuarioInicial,
                                 @Value("${ADMIN_CORREO:}") String correoInicial,
                                 @Value("${ADMIN_CONTRASENA:}") String contrasenaInicial) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.codificador = codificador;
        this.usuarioInicial = usuarioInicial;
        this.correoInicial = correoInicial;
        this.contrasenaInicial = contrasenaInicial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Decision decision = decidir(usuarios.count(), usuarioInicial, correoInicial, contrasenaInicial);

        switch (decision) {
            case YA_HAY_CUENTAS -> LOG.debug("La base ya tiene cuentas: no se crea ninguna inicial.");
            case SIN_CONFIGURAR -> LOG.warn(
                    "No hay ninguna cuenta en la base y no se configuró una inicial. "
                            + "El servicio atiende las rutas públicas, pero nadie puede gestionar. "
                            + "Defina ADMIN_USUARIO, ADMIN_CORREO y ADMIN_CONTRASENA y reinicie.");
            case CONTRASENA_CORTA -> LOG.warn(
                    "No se creó la cuenta inicial: ADMIN_CONTRASENA tiene menos de {} caracteres. "
                            + "Una cuenta con todos los permisos no se protege con una contraseña corta.",
                    LONGITUD_MINIMA);
            case CREAR -> crear();
        }
    }

    /**
     * Resuelve qué procede hacer.
     *
     * <p>Se expone con visibilidad de paquete y sin dependencias para poder
     * probarse sin levantar el contexto ni la base.</p>
     */
    static Decision decidir(long cuentasExistentes, String usuario, String correo, String contrasena) {
        if (cuentasExistentes > 0) {
            return Decision.YA_HAY_CUENTAS;
        }
        if (enBlanco(usuario) || enBlanco(correo) || enBlanco(contrasena)) {
            return Decision.SIN_CONFIGURAR;
        }
        if (contrasena.length() < LONGITUD_MINIMA) {
            return Decision.CONTRASENA_CORTA;
        }
        return Decision.CREAR;
    }

    private void crear() {
        Usuario administrador = new Usuario(
                usuarioInicial.trim(),
                correoInicial.trim(),
                codificador.encode(contrasenaInicial),
                "Cuenta", "de administración",
                LocalDateTime.now());

        // Administra y además tramita: en un despliegue recién hecho no hay
        // nadie más, y una cuenta que solo administra no podría responder una
        // solicitud que llegue mientras tanto.
        for (String codigo : List.of("ROLE_ADMIN", "ROLE_FUNCIONARIO")) {
            roles.findByCodigo(codigo).ifPresent(administrador::asignarRol);
        }

        usuarios.save(administrador);

        LOG.info("Cuenta de administración creada a partir del entorno: {}. "
                        + "Cambie la contraseña después del primer acceso.",
                administrador.getNombreUsuario());
    }

    private static boolean enBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
