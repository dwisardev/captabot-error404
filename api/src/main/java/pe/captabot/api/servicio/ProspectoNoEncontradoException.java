package pe.captabot.api.servicio;

public class ProspectoNoEncontradoException extends RuntimeException {

    public ProspectoNoEncontradoException(Long id) {
        super("No existe el prospecto " + id);
    }
}
