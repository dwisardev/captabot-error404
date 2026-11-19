package pe.captabot.api.chat;

import pe.captabot.api.modelo.Urgencia;

import java.math.BigDecimal;
import java.time.Instant;

/** Datos que el cliente va dejando en el chat. Se guardan en memoria mientras dura la conversación. */
class Conversacion {

    final String id;
    /** CHAT_WEB o TELEGRAM: se guarda en el prospecto para saber por dónde llegó. */
    final String canal;
    /** Identificador del cliente en su canal (chat de Telegram o número de WhatsApp), para el seguimiento (HU13). */
    String contacto;
    EstadoConversacion estado = EstadoConversacion.MENU;
    String nombre;
    String telefono;
    String servicio;
    BigDecimal presupuesto;
    Urgencia urgencia;
    Instant ultimaActividad = Instant.now();

    Conversacion(String id, String canal) {
        this.id = id;
        this.canal = canal;
    }

    void reiniciarDatos() {
        nombre = null;
        telefono = null;
        servicio = null;
        presupuesto = null;
        urgencia = null;
    }
}
