package pe.captabot.api.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.servicio.UsuarioService;

/** HU06: el panel valida aquí el correo y la contraseña del usuario. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService servicio;

    public AuthController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest solicitud) {
        return UsuarioResponse.desde(servicio.autenticar(solicitud.correo(), solicitud.contrasena()));
    }
}
