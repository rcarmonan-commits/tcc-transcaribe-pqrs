package co.edu.unicartagena.transcaribe.pqrs.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Prefijo institucional del número de radicado. */
@Validated
@ConfigurationProperties(prefix = "pqrs.radicado")
public record PropiedadesRadicado(

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2,5}$", message = "El prefijo del radicado debe ser de 2 a 5 letras mayúsculas")
        String prefijo) {
}
