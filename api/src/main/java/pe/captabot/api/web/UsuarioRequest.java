package pe.captabot.api.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar un usuario (HU07). Al editar, la contraseña es opcional. */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo admite hasta 150 caracteres")
        String correo,

        String contrasena,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
