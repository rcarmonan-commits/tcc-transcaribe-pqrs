package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Reporte de un evento operativo ocurrido sobre un vehículo de la flota. */
public record RegistrarIncidenciaPeticion(

        @NotBlank(message = "La placa del vehículo es obligatoria")
        @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$", message = "La placa debe tener el formato AAA999")
        String placaVehiculo,

        @Pattern(regexp = "^$|^[A-Za-z0-9-]{1,20}$", message = "El código de ruta no es válido")
        String codigoRuta,

        @NotBlank(message = "El tipo de incidencia es obligatorio")
        @Size(max = 40)
        String tipoIncidencia,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
        String descripcion,

        @NotNull(message = "La fecha y hora del hecho son obligatorias")
        LocalDateTime fechaHora,

        @Pattern(regexp = "^$|^[A-Za-z0-9-]{1,20}$", message = "El radicado asociado no es válido")
        String radicadoAsociado) {
}
