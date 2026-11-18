package pe.captabot.api.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.servicio.EtapaEmbudo;
import pe.captabot.api.servicio.ReporteSemanal;
import pe.captabot.api.servicio.ReporteService;
import pe.captabot.api.servicio.TasaConversion;

import java.time.LocalDate;
import java.util.List;

/** HU11 y HU12: datos para los reportes del panel. Las fechas van en formato AAAA-MM-DD. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/semanal")
    public List<ReporteSemanal> semanal(@RequestParam(defaultValue = "8") int semanas) {
        return servicio.porSemana(semanas);
    }

    @GetMapping("/embudo")
    public List<EtapaEmbudo> embudo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.embudo(desde, hasta);
    }

    @GetMapping("/conversion")
    public TasaConversion conversion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.conversion(desde, hasta);
    }
}
