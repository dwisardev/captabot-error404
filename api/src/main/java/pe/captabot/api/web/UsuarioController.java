package pe.captabot.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.modelo.Usuario;
import pe.captabot.api.servicio.UsuarioService;

import java.net.URI;
import java.util.List;

/** HU07: el administrador crea, edita y desactiva a los usuarios de ventas. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return UsuarioResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest solicitud) {
        Usuario usuario = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.getId()))
                .body(UsuarioResponse.desde(usuario));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest solicitud) {
        return UsuarioResponse.desde(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest solicitud) {
        return UsuarioResponse.desde(servicio.cambiarEstado(id, solicitud.activo()));
    }
}
