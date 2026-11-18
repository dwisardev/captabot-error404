package pe.captabot.api.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.servicio.ProspectoNoEncontradoException;
import pe.captabot.api.servicio.ProspectoService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del endpoint que usa watsonx Assistant (HU05). */
class ProspectoControllerTest {

    private ProspectoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ProspectoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProspectoController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    private static Prospecto prospectoDeEjemplo() {
        Prospecto p = new Prospecto();
        p.setId(1L);
        p.setNombre("María Quispe");
        p.setTelefono("987654321");
        p.setServicioInteres("Diseño de página web");
        p.setPresupuesto(new BigDecimal("6000"));
        p.setUrgencia(Urgencia.ALTA);
        p.setPuntaje(100);
        p.setCalificacion(Calificacion.CALIENTE);
        p.setEtapa(Etapa.NUEVO);
        p.setCreadoEn(OffsetDateTime.now());
        return p;
    }

    @Test
    void registraUnProspectoYDevuelveSuCalificacion() throws Exception {
        when(servicio.registrar(any())).thenReturn(prospectoDeEjemplo());

        mvc.perform(post("/api/prospectos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María Quispe","telefono":"987654321",
                                 "servicioInteres":"Diseño de página web","presupuesto":6000,"urgencia":"alta"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/prospectos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.calificacion").value("CALIENTE"))
                .andExpect(jsonPath("$.puntaje").value(100))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("Gracias, María.")));
    }

    @Test
    void rechazaUnTelefonoQueNoTiene9Digitos() throws Exception {
        mvc.perform(post("/api/prospectos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Jorge","telefono":"12345",
                                 "servicioInteres":"Mantenimiento","presupuesto":100,"urgencia":"baja"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.telefono").value("El teléfono debe tener 9 dígitos"));
    }

    @Test
    void rechazaSiFaltanDatosObligatorios() throws Exception {
        mvc.perform(post("/api/prospectos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Jorge\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.presupuesto").exists())
                .andExpect(jsonPath("$.detalles.urgencia").exists());
    }

    @Test
    void devuelve404SiElProspectoNoExiste() throws Exception {
        when(servicio.obtener(99L)).thenThrow(new ProspectoNoEncontradoException(99L));

        mvc.perform(get("/api/prospectos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe el prospecto 99"));
    }
}
