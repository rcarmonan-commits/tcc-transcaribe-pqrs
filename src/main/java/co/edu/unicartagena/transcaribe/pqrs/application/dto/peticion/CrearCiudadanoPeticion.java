package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro de un ciudadano.
 *
 * <p>Las restricciones no son cosmética: son la primera barrera de entrada del
 * servicio. Un DTO con validación estricta rechaza la carga maliciosa antes de
 * que llegue al dominio o a la base de datos (OWASP A03:2021). Las expresiones
 * regulares describen lo que se acepta, no lo que se prohíbe: una lista de
 * prohibidos siempre se queda corta frente a una codificación inesperada.</p>
 */
public record CrearCiudadanoPeticion(

        @NotBlank(message = "El tipo de documento es obligatorio")
        @Pattern(regexp = "CC|CE|TI|PAS|NIT", message = "Tipo de documento no reconocido")
        String tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "^[0-9]{5,20}$", message = "El número de documento debe tener entre 5 y 20 dígitos")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
        @Pattern(regexp = "^[\\p{L} .'-]+$",
                 message = "Los nombres solo admiten letras, espacios y los signos punto, apóstrofo y guion")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
        @Pattern(regexp = "^[\\p{L} .'-]+$",
                 message = "Los apellidos solo admiten letras, espacios y los signos punto, apóstrofo y guion")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 160, message = "El correo no puede superar 160 caracteres")
        String correo,

        @Pattern(regexp = "^$|^[0-9+ ()-]{7,20}$", message = "El teléfono no tiene un formato válido")
        String telefono) {
}
