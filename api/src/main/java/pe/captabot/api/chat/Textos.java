package pe.captabot.api.chat;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Utilidades para entender lo que escribe el cliente: sin tildes, sin signos y en minúsculas. */
final class Textos {

    private static final Set<String> SI = Set.of("si", "claro", "ok", "okay", "dale", "acepto", "bueno", "va",
            "correcto", "afirmativo", "porsupuesto", "deacuerdo", "yes");
    private static final Set<String> NO = Set.of("no", "nop", "nel", "negativo", "nunca", "despues", "luego");

    private Textos() {
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").trim();
    }

    static List<String> palabras(String texto) {
        String limpio = normalizar(texto);
        return limpio.isEmpty() ? List.of() : Arrays.asList(limpio.split(" "));
    }

    /** true si alguna palabra empieza con alguna de las raíces. */
    static boolean contiene(String texto, Set<String> raices) {
        return contar(texto, raices) > 0;
    }

    static int contar(String texto, Set<String> raices) {
        int total = 0;
        for (String palabra : palabras(texto)) {
            for (String raiz : raices) {
                if (palabra.startsWith(raiz)) {
                    total++;
                    break;
                }
            }
        }
        return total;
    }

    /** Interpreta un sí o un no. Devuelve null si no se entiende. */
    static Boolean siONo(String texto) {
        String limpio = normalizar(texto).replace("de acuerdo", "deacuerdo").replace("por supuesto", "porsupuesto");
        List<String> palabras = limpio.isEmpty() ? List.of() : Arrays.asList(limpio.split(" "));
        if (palabras.isEmpty()) {
            return null;
        }
        if (NO.contains(palabras.get(0))) {
            return false;
        }
        for (String palabra : palabras) {
            if (SI.contains(palabra)) {
                return true;
            }
        }
        return null;
    }

    /** Primer nombre con mayúscula inicial: "maría quispe" → "María". */
    static String primerNombre(String nombre) {
        String primero = nombre.trim().split("\\s+")[0];
        return primero.isEmpty() ? primero
                : primero.substring(0, 1).toUpperCase(Locale.ROOT) + primero.substring(1);
    }
}
