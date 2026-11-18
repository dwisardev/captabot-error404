package pe.captabot.api.chat;

import java.math.BigDecimal;
import java.time.Instant;

/** Datos que el cliente va dejando en el chat. Se guardan en memoria mientras dura la conversación. */
class Conversacion {

    final String id;
    EstadoConversacion estado = EstadoConversacion.MENU;
    String nombre;
    String telefono;
    String servicio;
    BigDecimal presupuesto;
    Instant ultimaActividad = Instant.now();

    Conversacion(String id) {
        this.id = id;
    }

    void reiniciarDatos() {
        nombre = null;
        telefono = null;
        servicio = null;
        presupuesto = null;
    }
}
