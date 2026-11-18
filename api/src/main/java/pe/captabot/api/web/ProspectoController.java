package pe.captabot.api.web;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import pe.captabot.api.servicio.FiltroProspectos;
import pe.captabot.api.servicio.ProspectoService;

import java.net.URI;
import java.time.LocalDate;

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

    /**
     * HU08: lista paginada para el panel, con los de mayor puntaje primero.
     * Filtros opcionales: calificacion, etapa, desde y hasta (AAAA-MM-DD).
     */
    @GetMapping
    public PaginaResponse<ProspectoResponse> listar(
            @RequestParam(required = false) Calificacion calificacion,
            @RequestParam(required = false) Etapa etapa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + ProspectoService.TAMANO_PAGINA) int tamano) {
        FiltroProspectos filtro = new FiltroProspectos(calificacion, etapa, desde, hasta);
        return PaginaResponse.desde(servicio.listar(filtro, pagina, tamano), ProspectoResponse::desde);
    }

    @GetMapping("/{id}")
    public ProspectoResponse obtener(@PathVariable Long id) {
        return ProspectoResponse.desde(servicio.obtener(id));
    }
}
