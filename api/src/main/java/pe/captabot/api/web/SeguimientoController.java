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
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.HistorialEtapa;
import pe.captabot.api.modelo.NotaSeguimiento;
import pe.captabot.api.servicio.SeguimientoService;

import java.net.URI;
import java.util.List;
import java.util.Map;

/** HU09 y HU10: etapa, historial y notas de un prospecto. Las notas no tienen opción de borrar. */
@RestController
@RequestMapping("/api/prospectos/{id}")
public class SeguimientoController {

    private final SeguimientoService servicio;

    public SeguimientoController(SeguimientoService servicio) {
        this.servicio = servicio;
    }

    @PatchMapping("/etapa")
    public ProspectoResponse cambiarEtapa(@PathVariable Long id, @Valid @RequestBody CambioEtapaRequest solicitud) {
        Etapa etapa = Etapa.desdeTexto(solicitud.etapa());
        return ProspectoResponse.desde(servicio.cambiarEtapa(id, etapa, solicitud.usuarioId()));
    }

    @GetMapping("/historial")
    public List<HistorialResponse> historial(@PathVariable Long id) {
        List<HistorialEtapa> cambios = servicio.historial(id);
        Map<Long, String> nombres = servicio.nombresDeUsuarios(
                cambios.stream().map(HistorialEtapa::getUsuarioId).toList());
        return cambios.stream().map(h -> HistorialResponse.desde(h, nombres)).toList();
    }

    @GetMapping("/notas")
    public List<NotaResponse> notas(@PathVariable Long id) {
        List<NotaSeguimiento> lista = servicio.notas(id);
        Map<Long, String> nombres = servicio.nombresDeUsuarios(
                lista.stream().map(NotaSeguimiento::getUsuarioId).toList());
        return lista.stream().map(n -> NotaResponse.desde(n, nombres)).toList();
    }

    @PostMapping("/notas")
    public ResponseEntity<NotaResponse> agregarNota(@PathVariable Long id, @Valid @RequestBody NotaRequest solicitud) {
        NotaSeguimiento nota = servicio.agregarNota(id, solicitud.usuarioId(), solicitud.contenido());
        NotaResponse respuesta = NotaResponse.desde(nota, servicio.nombresDeUsuarios(List.of(nota.getUsuarioId())));
        return ResponseEntity.created(URI.create("/api/prospectos/" + id + "/notas/" + nota.getId())).body(respuesta);
    }

    @PutMapping("/notas/{notaId}")
    public NotaResponse editarNota(@PathVariable Long id, @PathVariable Long notaId,
                                   @Valid @RequestBody NotaRequest solicitud) {
        NotaSeguimiento nota = servicio.editarNota(id, notaId, solicitud.usuarioId(), solicitud.contenido());
        return NotaResponse.desde(nota, servicio.nombresDeUsuarios(List.of(nota.getUsuarioId())));
    }
}
