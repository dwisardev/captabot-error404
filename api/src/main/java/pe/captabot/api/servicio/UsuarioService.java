package pe.captabot.api.servicio;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Rol;
import pe.captabot.api.modelo.Usuario;
import pe.captabot.api.repositorio.UsuarioRepository;
import pe.captabot.api.web.UsuarioRequest;

import java.util.List;
import java.util.Locale;

/** Inicio de sesión (HU06) y gestión de usuarios del panel (HU07). */
@Service
public class UsuarioService {

    static final int LONGITUD_MINIMA_CONTRASENA = 8;

    private final UsuarioRepository repositorio;
    private final PasswordEncoder cifrador;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder cifrador) {
        this.repositorio = repositorio;
        this.cifrador = cifrador;
    }

    /** Devuelve el usuario si el correo y la contraseña son correctos y está activo. */
    @Transactional(readOnly = true)
    public Usuario autenticar(String correo, String contrasena) {
        Usuario usuario = repositorio.findByCorreoIgnoreCase(normalizarCorreo(correo))
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || contrasena == null
                || !cifrador.matches(contrasena, usuario.getContrasenaHash())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repositorio.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
    }

    @Transactional
    public Usuario crear(UsuarioRequest solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo);
        }
        if (solicitud.contrasena() == null || solicitud.contrasena().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(Rol.desdeTexto(solicitud.rol()));
        usuario.setContrasenaHash(cifrar(solicitud.contrasena()));
        usuario.setActivo(true);
        return repositorio.save(usuario);
    }

    /** Edita nombre, correo y rol. La contraseña solo cambia si se envía una nueva. */
    @Transactional
    public Usuario actualizar(Long id, UsuarioRequest solicitud) {
        Usuario usuario = obtener(id);
        String correo = normalizarCorreo(solicitud.correo());
        if (repositorio.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo);
        }
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(Rol.desdeTexto(solicitud.rol()));
        if (solicitud.contrasena() != null && !solicitud.contrasena().isBlank()) {
            usuario.setContrasenaHash(cifrar(solicitud.contrasena()));
        }
        return repositorio.save(usuario);
    }

    /** Activa o desactiva un usuario. Un usuario desactivado no puede iniciar sesión. */
    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = obtener(id);
        usuario.setActivo(activo);
        return repositorio.save(usuario);
    }

    private String cifrar(String contrasena) {
        if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres");
        }
        return cifrador.encode(contrasena);
    }

    private static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
