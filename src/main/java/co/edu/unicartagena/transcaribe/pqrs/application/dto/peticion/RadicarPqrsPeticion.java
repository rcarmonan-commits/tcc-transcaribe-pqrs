package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Radicación de una nueva solicitud ciudadana.
 *
 * <p>El ciudadano llega anidado y no por identificador: una persona que radica
 * por primera vez no tiene todavía un registro, y obligarla a crearse antes
 * añadiría un viaje y una condición de carrera. El caso de uso resuelve si debe
 * crear el ciudadano o reutilizar el existente.</p>
 *
 * <p>Ni el radicado, ni la prioridad, ni la fecha de vencimiento se aceptan
 * desde fuera: son datos derivados que calcula el servidor. Permitir que el
 * cliente los envíe sería una asignación masiva de propiedades, el patrón que
 * OWASP recoge en A08:2021 sobre fallas de integridad del software y los datos.</p>
 */
public record RadicarPqrsPeticion(

        @NotNull(message = "Los datos del ciudadano son obligatorios")
        @Valid
        CrearCiudadanoPeticion ciudadano,

        @NotBlank(message = "El tipo de solicitud es obligatorio")
        @Size(max = 30)
        String tipoSolicitud,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 40)
        String categoria,

        @NotBlank(message = "El canal de recepción es obligatorio")
        @Size(max = 30)
        String canal,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(min = 10, max = 180, message = "El asunto debe tener entre 10 y 180 caracteres")
        String asunto,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 20, max = 4000, message = "La descripción debe tener entre 20 y 4000 caracteres")
        String descripcion,

        @Pattern(regexp = "^$|^[A-Za-z0-9-]{1,20}$", message = "El código de ruta no es válido")
        String codigoRuta,

        @Pattern(regexp = "^$|^[A-Za-z0-9]{5,10}$", message = "La placa no es válida")
        String placaVehiculo) {
}
