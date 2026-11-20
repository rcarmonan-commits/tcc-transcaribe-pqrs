package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta de una ruta del sistema integrado. */
public record CrearRutaPeticion(

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,20}$", message = "El código de ruta no es válido")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @NotBlank(message = "El origen es obligatorio")
        @Size(max = 120)
        String origen,

        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 120)
        String destino) {
}
