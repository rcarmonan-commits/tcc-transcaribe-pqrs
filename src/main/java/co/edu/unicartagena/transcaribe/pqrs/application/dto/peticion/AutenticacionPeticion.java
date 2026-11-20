package co.edu.unicartagena.transcaribe.pqrs.application.dto.peticion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de acceso de un funcionario.
 *
 * <p>Este registro nunca se escribe en una traza ni se devuelve en una
 * respuesta. La contraseña viaja solo de entrada y muere en el verificador de
 * BCrypt. El {@code toString} está sobrescrito justamente para que un volcado
 * accidental del objeto no termine imprimiendo la credencial en un archivo de
 * registro, que es una de las formas más tontas de perder un secreto.</p>
 */
public record AutenticacionPeticion(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 60)
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 120)
        String contrasena) {

    @Override
    public String toString() {
        return "AutenticacionPeticion[nombreUsuario=%s, contrasena=***]".formatted(nombreUsuario);
    }
}
