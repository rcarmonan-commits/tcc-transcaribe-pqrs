package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Respuesta de fondo con la que se atiende la solicitud del ciudadano. */
public record ResponderPqrsPeticion(

        @NotBlank(message = "La respuesta es obligatoria")
        @Size(min = 20, max = 4000, message = "La respuesta debe tener entre 20 y 4000 caracteres")
        String respuesta) {
}
