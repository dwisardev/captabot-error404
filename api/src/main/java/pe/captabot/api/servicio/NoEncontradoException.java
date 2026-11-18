package pe.captabot.api.servicio;

/** El recurso pedido no existe. La API responde 404. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
