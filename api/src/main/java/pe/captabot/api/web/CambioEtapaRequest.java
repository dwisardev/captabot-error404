package pe.captabot.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** HU09: nueva etapa y usuario que hace el cambio. */
public record CambioEtapaRequest(
        @NotBlank(message = "La etapa es obligatoria") String etapa,
        @NotNull(message = "El usuario es obligatorio") Long usuarioId
) {
}
