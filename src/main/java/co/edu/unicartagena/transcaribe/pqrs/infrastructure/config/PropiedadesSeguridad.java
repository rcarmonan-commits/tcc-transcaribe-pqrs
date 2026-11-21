package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros de seguridad del servicio.
 *
 * <p>Están validados y el arranque falla si alguno falta o es demasiado débil.
 * Es deliberado: un servicio que arranca con una clave de firma vacía parece
 * sano y no lo está, y el fallo solo se descubre cuando alguien falsifica un
 * testigo. Mejor que no arranque (OWASP A05:2021, configuración incorrecta).</p>
 */
@Validated
@ConfigurationProperties(prefix = "pqrs.seguridad")
public record PropiedadesSeguridad(Jwt jwt,
                                   @Min(3) int intentosMaximos,
                                   @Min(1) int bloqueoMinutos,
                                   Cors cors,
                                   LimitePeticiones limitePeticiones) {

    /** Longitud mínima de la clave HMAC para HS256, en bytes. */
    public static final int LONGITUD_MINIMA_SECRETO = 32;

    public record Jwt(@NotBlank String secreto,
                      @Min(5) int expiracionMinutos,
                      @NotBlank String emisor) {

        public Jwt {
            if (secreto != null && secreto.getBytes().length < LONGITUD_MINIMA_SECRETO) {
                throw new IllegalStateException(
                        "La clave de firma JWT debe tener al menos %d bytes. Defina la variable de entorno JWT_SECRET."
                                .formatted(LONGITUD_MINIMA_SECRETO));
            }
        }
    }

    public record Cors(List<String> origenesPermitidos) {

        public Cors {
            origenesPermitidos = (origenesPermitidos == null) ? List.of() : List.copyOf(origenesPermitidos);
            if (origenesPermitidos.contains("*")) {
                throw new IllegalStateException(
                        "No se admite '*' como origen permitido: un servicio que expone datos personales "
                                + "debe declarar los orígenes de forma explícita.");
            }
        }
    }

    public record LimitePeticiones(boolean habilitado, @Min(1) int maximoPorMinuto) {
    }
}
