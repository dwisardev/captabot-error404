package pe.captabot.api.chat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee el presupuesto que escribe el cliente: "2500", "S/ 2,500", "2.500 soles", "2 mil" o "1.5k". */
final class Montos {

    private static final Pattern NUMERO = Pattern.compile("\\d[\\d.,]*");
    private static final BigDecimal MAXIMO = new BigDecimal("1000000000");

    private Montos() {
    }

    static Optional<BigDecimal> leer(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String t = texto.toLowerCase(Locale.ROOT)
                .replace("s/.", " ").replace("s/", " ").replace("soles", " ").replace("pen", " ");
        Matcher m = NUMERO.matcher(t);
        if (!m.find()) {
            return Optional.empty();
        }
        String numero = m.group().replaceAll("[.,]+$", "");
        if (numero.contains(",") && numero.contains(".")) {
            numero = numero.replace(",", "");                       // 2,500.50
        } else if (numero.contains(",")) {
            int decimales = numero.length() - numero.lastIndexOf(',') - 1;
            numero = decimales == 3 ? numero.replace(",", "") : numero.replace(',', '.');
        } else if (numero.indexOf('.') != numero.lastIndexOf('.')
                || (numero.contains(".") && numero.length() - numero.lastIndexOf('.') - 1 == 3)) {
            numero = numero.replace(".", "");                       // 2.500 o 1.000.000
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(numero);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        String resto = t.substring(m.end()).trim();
        if (resto.startsWith("mil") || resto.startsWith("k")) {
            valor = valor.multiply(BigDecimal.valueOf(1000));
        }
        if (valor.compareTo(MAXIMO) > 0) {
            return Optional.empty();
        }
        return Optional.of(valor.setScale(2, RoundingMode.HALF_UP));
    }
}
