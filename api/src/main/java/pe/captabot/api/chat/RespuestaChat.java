package pe.captabot.api.chat;

import java.util.List;

/**
 * Lo que el chat muestra al cliente: uno o más mensajes y, si corresponde, botones de respuesta rápida.
 * registrado es true en el mensaje en que se guardó el prospecto. pideTelefono es true cuando el chat espera
 * el celular (Telegram muestra el botón «Compartir mi número»).
 */
public record RespuestaChat(String sesionId, List<String> mensajes, List<String> opciones, boolean registrado,
                            boolean pideTelefono) {
}
