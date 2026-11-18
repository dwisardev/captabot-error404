package pe.captabot.api.chat;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Responde las preguntas frecuentes sin servicios externos: busca las palabras clave de cada tema.
 * Para cambiar las respuestas de la demo, edita la lista PREGUNTAS.
 */
@Component
public class PreguntasLocales implements MotorPreguntas {

    static final List<PreguntaFrecuente> PREGUNTAS = List.of(
            new PreguntaFrecuente("Horario",
                    Set.of("horari", "hora", "atiend", "abre", "abiert", "cierr", "sabad", "doming", "feriad"),
                    "Atendemos de lunes a viernes de 9:00 a. m. a 6:00 p. m. y los sábados de 9:00 a. m. "
                            + "a 1:00 p. m. Por este chat te respondo a cualquier hora."),
            new PreguntaFrecuente("Ubicación",
                    Set.of("ubic", "direcc", "donde", "local", "oficin", "distrit", "llego", "queda"),
                    "Nuestra oficina de ejemplo está en Av. Principal 123, Lima. También atendemos de forma "
                            + "virtual a todo el Perú."),
            new PreguntaFrecuente("Precios",
                    Set.of("preci", "cuest", "cobr", "tarif", "costo", "vale"),
                    "El precio depende de lo que necesites. Como referencia, una página web informativa parte "
                            + "de S/ 800. Si me das algunos datos, un asesor te envía una cotización exacta.", 2),
            new PreguntaFrecuente("Servicios",
                    Set.of("servici", "ofrec", "dedic", "hacen", "pagina", "web", "marketing", "redes", "soport"),
                    "Ofrecemos diseño de páginas web, marketing digital en redes sociales y soporte técnico "
                            + "para empresas."),
            new PreguntaFrecuente("Formas de pago",
                    Set.of("pago", "pagar", "pagu", "yape", "plin", "tarjet", "transfer", "cuota", "efectiv", "deposit"),
                    "Aceptamos transferencia bancaria, Yape, Plin y tarjetas. En proyectos grandes puedes pagar "
                            + "50 % al inicio y 50 % a la entrega."),
            new PreguntaFrecuente("Tiempo de entrega",
                    Set.of("demor", "tiempo", "plazo", "entreg", "dias", "rapid", "tard", "listo"),
                    "Una página web informativa toma entre 7 y 15 días hábiles. Los demás servicios se acuerdan "
                            + "según el alcance del proyecto."),
            new PreguntaFrecuente("Garantía",
                    Set.of("garant", "falla", "mantenim", "despues", "problem", "arregl"),
                    "Todos nuestros trabajos incluyen 30 días de soporte sin costo después de la entrega. Luego "
                            + "puedes contratar un plan de mantenimiento mensual."));

    @Override
    public Optional<String> responder(String pregunta) {
        PreguntaFrecuente mejor = null;
        int mejorPuntaje = 0;
        for (PreguntaFrecuente p : PREGUNTAS) {
            int puntaje = Textos.contar(pregunta, p.raices()) * p.peso();
            if (puntaje > mejorPuntaje) {
                mejor = p;
                mejorPuntaje = puntaje;
            }
        }
        return Optional.ofNullable(mejor).map(PreguntaFrecuente::respuesta);
    }
}
