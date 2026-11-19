package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Transición de estado solicitada por un funcionario sobre una PQRS. */
public record CambiarEstadoPeticion(

        @NotBlank(message = "El estado destino es obligatorio")
        @Pattern(regexp = "RADICADA|EN_TRAMITE|RESUELTA|CERRADA|VENCIDA",
                 message = "Estado destino no reconocido")
        String estadoDestino,

        @NotBlank(message = "La observación es obligatoria")
        @Size(min = 5, max = 1000, message = "La observación debe tener entre 5 y 1000 caracteres")
        String observacion) {
}
