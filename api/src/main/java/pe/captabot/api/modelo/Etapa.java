package pe.captabot.api.modelo;

import java.util.Locale;

/** Etapas del seguimiento comercial (HU09). */
public enum Etapa {
    NUEVO, CONTACTADO, COTIZADO, CERRADO_GANADO, CERRADO_PERDIDO;

    /** @throws IllegalArgumentException si el texto no es una etapa válida */
    public static Etapa desdeTexto(String texto) {
        if (texto != null) {
            String limpio = texto.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            for (Etapa e : values()) {
                if (e.name().equals(limpio)) {
                    return e;
                }
            }
        }
        throw new IllegalArgumentException(
                "La etapa debe ser NUEVO, CONTACTADO, COTIZADO, CERRADO_GANADO o CERRADO_PERDIDO");
    }
}
