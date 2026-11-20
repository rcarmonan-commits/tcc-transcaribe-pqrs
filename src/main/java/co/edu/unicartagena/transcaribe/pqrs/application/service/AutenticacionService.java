package co.edu.unicartagena.transcaribe.pqrs.application.service;

import co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion.AutenticacionPeticion;
import co.edu.unicartagena.transcaribe.pqrs.application.dto.respuesta.AutenticacionRespuesta;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Rol;
import co.edu.unicartagena.transcaribe.pqrs.domain.model.Usuario;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.AuditoriaPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.port.UsuarioRepositoryPort;
import co.edu.unicartagena.transcaribe.pqrs.domain.service.Reloj;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import co.edu.unicartagena.transcaribe.pqrs.infrastructure.security.ProveedorJwt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticación de funcionarios (CU-11).
 *
 * <p>Tres decisiones que mitigan OWASP A07:2021:</p>
 *
 * <ol>
 *   <li><b>Un único mensaje de error</b> para usuario inexistente, contraseña
 *       incorrecta y cuenta bloqueada. Distinguirlos convierte el formulario en
 *       un verificador de nombres de usuario válidos.</li>
 *   <li><b>Verificación de la contraseña incluso cuando el usuario no existe.</b>
 *       Sin ese trabajo artificial, la respuesta para un usuario inexistente
 *       sería notablemente más rápida que para uno real, y el tiempo delata lo
 *       que el mensaje oculta.</li>
 *   <li><b>Bloqueo temporal tras varios fallos</b>, delegado en el agregado
 *       {@link Usuario}, que es quien conoce su propio estado.</li>
 * </ol>
 */
@Service
public class AutenticacionService {

    /**
     * Resumen BCrypt real, con el mismo factor de trabajo que el de producción
     * (12), de un valor aleatorio que nadie conoce. Solo existe para consumir
     * el mismo tiempo que una verificación auténtica cuando el usuario no
     * existe. Ninguna contraseña puede coincidir con él.
     */
    private static final String HASH_SENUELO =
            "$2b$12$uR1KXJDbakP36mN2xSrIVeUPT/PPg8reH94rIFSez4FHbqKuemMtS";

    private static final String ERROR_GENERICO = "Credenciales inválidas";

    private final UsuarioRepositoryPort usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final ProveedorJwt proveedorJwt;
    private final AuditoriaPort auditoria;
    private final PropiedadesSeguridad propiedades;
    private final Reloj reloj;

    public AutenticacionService(UsuarioRepositoryPort usuarioRepositorio,
                                PasswordEncoder codificador,
                                ProveedorJwt proveedorJwt,
                                AuditoriaPort auditoria,
                                PropiedadesSeguridad propiedades,
                                Reloj reloj) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.proveedorJwt = proveedorJwt;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public AutenticacionRespuesta iniciarSesion(AutenticacionPeticion peticion, String ipOrigen) {
        LocalDateTime ahora = reloj.ahora();

        Optional<Usuario> posible = usuarioRepositorio.buscarPorNombreUsuario(peticion.nombreUsuario());

        if (posible.isEmpty()) {
            // Trabajo equivalente al de una verificación real: iguala el tiempo
            // de respuesta y evita la enumeración de usuarios por latencia.
            codificador.matches(peticion.contrasena(), HASH_SENUELO);
            registrarFallo(peticion.nombreUsuario(), ipOrigen);
            throw new BadCredentialsException(ERROR_GENERICO);
        }

        Usuario usuario = posible.get();

        if (!usuario.puedeAutenticarse(ahora)) {
            registrarFallo(peticion.nombreUsuario(), ipOrigen);
            throw new BadCredentialsException(ERROR_GENERICO);
        }

        if (!codificador.matches(peticion.contrasena(), usuario.getHashContrasena())) {
            usuario.registrarIntentoFallido(
                    propiedades.intentosMaximos(), propiedades.bloqueoMinutos(), ahora);
            usuarioRepositorio.guardar(usuario);
            registrarFallo(peticion.nombreUsuario(), ipOrigen);
            throw new BadCredentialsException(ERROR_GENERICO);
        }

        usuario.registrarAutenticacionExitosa();
        usuarioRepositorio.guardar(usuario);

        List<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).sorted().toList();
        String token = proveedorJwt.generar(usuario.getNombreUsuario(), roles);

        auditoria.registrar(usuario.getNombreUsuario(), "INICIO_SESION",
                "/api/v1/autenticacion/iniciar-sesion", ipOrigen, true);

        return AutenticacionRespuesta.bearer(
                token,
                usuario.getNombreUsuario(),
                usuario.nombreCompleto(),
                roles,
                proveedorJwt.expiracionDe(ahora));
    }

    private void registrarFallo(String nombreUsuario, String ipOrigen) {
        auditoria.registrar(nombreUsuario, "INICIO_SESION_FALLIDO",
                "/api/v1/autenticacion/iniciar-sesion", ipOrigen, false);
    }
}
