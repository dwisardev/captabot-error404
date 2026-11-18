package pe.captabot.api.servicio;

/** El usuario no puede hacer esta acción, por ejemplo editar la nota de otro. La API responde 403. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
