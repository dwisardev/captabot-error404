package pe.captabot.api.modelo;

import java.util.Locale;

/** Rol de un usuario del panel (HU07). */
public enum Rol {
    ADMINISTRADOR, VENDEDOR;

    /** @throws IllegalArgumentException si el texto no es ADMINISTRADOR o VENDEDOR */
    public static Rol desdeTexto(String texto) {
        if (texto != null) {
            String limpio = texto.trim().toUpperCase(Locale.ROOT);
            for (Rol r : values()) {
                if (r.name().equals(limpio)) {
                    return r;
                }
            }
        }
        throw new IllegalArgumentException("El rol debe ser ADMINISTRADOR o VENDEDOR");
    }
}
