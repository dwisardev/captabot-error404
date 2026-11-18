package pe.captabot.api.servicio;

/** El dato ya existe, por ejemplo un correo repetido. La API responde 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
