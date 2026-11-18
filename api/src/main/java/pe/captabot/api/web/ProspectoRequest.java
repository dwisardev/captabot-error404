package pe.captabot.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Datos que envía watsonx Assistant al registrar un prospecto (HU04 y HU05). */
public record ProspectoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @NotBlank(message = "El servicio de interés es obligatorio")
        @Size(max = 120, message = "El servicio admite hasta 120 caracteres")
        String servicioInteres,

        @NotNull(message = "El presupuesto es obligatorio")
        @PositiveOrZero(message = "El presupuesto no puede ser negativo")
        BigDecimal presupuesto,

        @NotBlank(message = "La urgencia es obligatoria")
        String urgencia
) {
}
