package co.edu.unicartagena.transcaribe.pqrs.infrastructure.security;

import co.edu.unicartagena.transcaribe.pqrs.infrastructure.config.PropiedadesSeguridad;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emisión y verificación de testigos JWT.
 *
 * <p>Decisiones de seguridad que conviene poder defender:</p>
 *
 * <ul>
 *   <li><b>Algoritmo fijado en el servidor.</b> La firma es HS256 y la
 *       verificación exige esa clave; no se confía en la cabecera
 *       {@code alg} del testigo recibido. Aceptarla es lo que habilita el
 *       ataque clásico de degradar el algoritmo a {@code none}.</li>
 *   <li><b>Emisor verificado.</b> Un testigo firmado con la misma clave pero
 *       emitido por otro servicio se rechaza.</li>
 *   <li><b>Vida corta.</b> Treinta minutos por omisión acotan la ventana de
 *       uso de un testigo robado, ya que un JWT no se puede revocar.</li>
 *   <li><b>Sin datos personales en la carga útil.</b> El cuerpo de un JWT va
 *       firmado, no cifrado: cualquiera que lo intercepte lo lee.</li>
 * </ul>
 */
@Component
public class ProveedorJwt {

    private static final Logger LOG = LoggerFactory.getLogger(ProveedorJwt.class);
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey clave;
    private final int expiracionMinutos;
    private final String emisor;

    public ProveedorJwt(PropiedadesSeguridad propiedades) {
        this.clave = Keys.hmacShaKeyFor(propiedades.jwt().secreto().getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = propiedades.jwt().expiracionMinutos();
        this.emisor = propiedades.jwt().emisor();
    }

    /**
     * Emite un testigo firmado.
     *
     * <p>Las marcas de emisión y expiración se toman del reloj del sistema y no
     * del {@code Reloj} del dominio. Es la única excepción deliberada a esa
     * regla: la vigencia de un JWT la verifica cualquier receptor contra su
     * propio reloj, de modo que fecharlo con un tiempo simulado produciría un
     * testigo inválido fuera de la prueba que lo creó.</p>
     */
    public String generar(String nombreUsuario, List<String> roles) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expira = ahora.plusMinutes(expiracionMinutos);

        return Jwts.builder()
                .subject(nombreUsuario)
                .issuer(emisor)
                .claim(CLAIM_ROLES, roles)
                .issuedAt(aDate(ahora))
                .expiration(aDate(expira))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public LocalDateTime expiracionDe(LocalDateTime desde) {
        return desde.plusMinutes(expiracionMinutos);
    }

    /**
     * Verifica firma, emisor y vigencia.
     *
     * @return las reclamaciones si el testigo es válido; vacío en cualquier
     *         otro caso. No se distingue el motivo hacia fuera: decirle al
     *         cliente si falló la firma o la fecha le da información gratis.
     */
    public Optional<Claims> verificar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(emisor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            LOG.debug("Testigo expirado");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            LOG.warn("Testigo rechazado: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public List<String> rolesDe(Claims claims) {
        Object valor = claims.get(CLAIM_ROLES);
        if (valor instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Date aDate(LocalDateTime fecha) {
        return Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant());
    }
}
