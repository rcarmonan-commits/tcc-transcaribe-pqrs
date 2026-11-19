package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/** Alta de un vehículo en la flota. */
public record CrearVehiculoPeticion(

        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$", message = "La placa debe tener el formato AAA999")
        String placa,

        @NotBlank(message = "El número interno es obligatorio")
        @Pattern(regexp = "^[0-9]{3,10}$", message = "El número interno debe ser numérico")
        String numeroInterno,

        @NotBlank(message = "El tipo de vehículo es obligatorio")
        @Pattern(regexp = "ARTICULADO|PADRON|ALIMENTADOR", message = "Tipo de vehículo no reconocido")
        String tipoVehiculo,

        @NotNull(message = "La fecha de vinculación es obligatoria")
        @Past(message = "La fecha de vinculación debe ser anterior a hoy")
        LocalDate fechaVinculacion) {
}
