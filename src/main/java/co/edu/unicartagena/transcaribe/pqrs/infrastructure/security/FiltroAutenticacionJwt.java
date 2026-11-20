package co.edu.unicartagena.transcaribe.pqrs.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica cada petición a partir del testigo JWT.
 *
 * <p>Extiende {@link OncePerRequestFilter} y no un filtro cualquiera porque un
 * reenvío interno volvería a ejecutar la lógica y podría dejar el contexto en
 * un estado incoherente.</p>
 *
 * <p>Si el testigo no es válido, el filtro no lanza ni responde: simplemente no
 * autentica y deja pasar la petición sin credenciales. La decisión de denegar
 * corresponde a la cadena de autorización, que es quien sabe si el recurso
 * requería permisos. Repartir esa decisión en dos sitios es como se terminan
 * abriendo rutas sin querer.</p>
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final ProveedorJwt proveedorJwt;

    public FiltroAutenticacionJwt(ProveedorJwt proveedorJwt) {
        this.proveedorJwt = proveedorJwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena) throws ServletException, IOException {

        extraerToken(peticion)
                .flatMap(proveedorJwt::verificar)
                .ifPresent(claims -> autenticar(claims, peticion));

        cadena.doFilter(peticion, respuesta);
    }

    private java.util.Optional<String> extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(CABECERA);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return java.util.Optional.empty();
        }
        String token = cabecera.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    private void autenticar(Claims claims, HttpServletRequest peticion) {
        List<SimpleGrantedAuthority> autoridades = proveedorJwt.rolesDe(claims).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, autoridades);
        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
