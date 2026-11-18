package pe.captabot.api.servicio;

public class ProspectoNoEncontradoException extends NoEncontradoException {

    public ProspectoNoEncontradoException(Long id) {
        super("No existe el prospecto " + id);
    }
}
