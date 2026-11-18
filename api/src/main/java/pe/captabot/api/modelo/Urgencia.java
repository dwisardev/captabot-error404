package pe.captabot.api.modelo;

import java.text.Normalizer;
import java.util.Locale;

/** Urgencia que el prospecto indica en el chat. */
public enum Urgencia {
    ALTA, MEDIA, BAJA;

    /**
     * Convierte el texto que llega del asistente ("alta", " Media ", "BAJA") en una urgencia.
     *
     * @throws IllegalArgumentException si el texto no es ALTA, MEDIA o BAJA
     */
    public static Urgencia desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La urgencia es obligatoria");
        }
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        for (Urgencia u : values()) {
            if (u.name().equals(limpio)) {
                return u;
            }
        }
        throw new IllegalArgumentException("La urgencia debe ser ALTA, MEDIA o BAJA");
    }
}
