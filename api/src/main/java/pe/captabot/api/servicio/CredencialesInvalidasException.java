package pe.captabot.api.servicio;

/** Correo o contraseña incorrectos, o usuario desactivado. La API responde 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
