package pe.captabot.api.web;

import pe.captabot.api.modelo.Usuario;

import java.time.OffsetDateTime;

/** Usuario sin su contraseña. */
public record UsuarioResponse(Long id, String nombre, String correo, String rol, boolean activo,
                              OffsetDateTime creadoEn) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().name(), u.isActivo(),
                u.getCreadoEn());
    }
}
