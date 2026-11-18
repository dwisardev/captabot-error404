package pe.captabot.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HU10: texto de la nota y su autor. */
public record NotaRequest(
        @NotNull(message = "El usuario es obligatorio") Long usuarioId,
        @NotBlank(message = "La nota no puede estar vacía")
        @Size(max = 2000, message = "La nota admite hasta 2000 caracteres")
        String contenido
) {
}
