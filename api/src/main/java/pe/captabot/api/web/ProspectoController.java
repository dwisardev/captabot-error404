package pe.captabot.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.servicio.ProspectoService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/prospectos")
public class ProspectoController {

    private final ProspectoService servicio;

    public ProspectoController(ProspectoService servicio) {
        this.servicio = servicio;
    }

    /** HU05: watsonx Assistant envía aquí los datos que dejó el prospecto en el chat. */
    @PostMapping
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody ProspectoRequest solicitud) {
        Prospecto p = servicio.registrar(solicitud);
        String primerNombre = p.getNombre().split("\\s+")[0];
        RegistroResponse respuesta = new RegistroResponse(
                p.getId(),
                p.getCalificacion().name(),
                p.getPuntaje(),
                "Gracias, " + primerNombre + ". Registramos tus datos y un asesor te contactará pronto.");
        return ResponseEntity.created(URI.create("/api/prospectos/" + p.getId())).body(respuesta);
    }

    /** Lista de prospectos para el panel, con filtros opcionales. */
    @GetMapping
    public List<ProspectoResponse> listar(@RequestParam(required = false) Calificacion calificacion,
                                          @RequestParam(required = false) Etapa etapa) {
        return servicio.listar(calificacion, etapa).stream().map(ProspectoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ProspectoResponse obtener(@PathVariable Long id) {
        return ProspectoResponse.desde(servicio.obtener(id));
    }
}
