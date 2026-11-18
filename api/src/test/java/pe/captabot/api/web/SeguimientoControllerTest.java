package pe.captabot.api.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.NotaSeguimiento;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.servicio.AccesoDenegadoException;
import pe.captabot.api.servicio.SeguimientoService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del cambio de etapa (HU09) y de las notas (HU10). */
class SeguimientoControllerTest {

    private SeguimientoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(SeguimientoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SeguimientoController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    private static Prospecto prospecto(Etapa etapa) {
        Prospecto p = new Prospecto();
        p.setId(1L);
        p.setNombre("María Quispe");
        p.setTelefono("987654321");
        p.setServicioInteres("Diseño web");
        p.setPresupuesto(new BigDecimal("6000"));
        p.setUrgencia(Urgencia.ALTA);
        p.setPuntaje(100);
        p.setCalificacion(Calificacion.CALIENTE);
        p.setEtapa(etapa);
        p.setCreadoEn(OffsetDateTime.now());
        return p;
    }

    @Test
    void cambiaLaEtapaAunqueLlegueEnMinusculas() throws Exception {
        when(servicio.cambiarEtapa(1L, Etapa.CONTACTADO, 2L)).thenReturn(prospecto(Etapa.CONTACTADO));

        mvc.perform(patch("/api/prospectos/1/etapa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etapa\":\"contactado\",\"usuarioId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("CONTACTADO"));
    }

    @Test
    void etapaDesconocidaDevuelve400() throws Exception {
        mvc.perform(patch("/api/prospectos/1/etapa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etapa\":\"archivado\",\"usuarioId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.startsWith("La etapa debe ser")));
    }

    @Test
    void listaLasNotasConElNombreDelAutor() throws Exception {
        NotaSeguimiento nota = new NotaSeguimiento();
        nota.setId(10L);
        nota.setProspectoId(1L);
        nota.setUsuarioId(2L);
        nota.setContenido("Pidió la cotización por correo");
        nota.setCreadoEn(OffsetDateTime.now());
        when(servicio.notas(1L)).thenReturn(List.of(nota));
        when(servicio.nombresDeUsuarios(any())).thenReturn(Map.of(2L, "Ana Ruiz"));

        mvc.perform(get("/api/prospectos/1/notas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contenido").value("Pidió la cotización por correo"))
                .andExpect(jsonPath("$[0].autor").value("Ana Ruiz"))
                .andExpect(jsonPath("$[0].autorId").value(2));
    }

    @Test
    void editarLaNotaDeOtroUsuarioDevuelve403() throws Exception {
        when(servicio.editarNota(1L, 10L, 3L, "Otro texto"))
                .thenThrow(new AccesoDenegadoException("Solo el autor puede editar la nota"));

        mvc.perform(put("/api/prospectos/1/notas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":3,\"contenido\":\"Otro texto\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo el autor puede editar la nota"));
    }

    @Test
    void noAceptaNotasVacias() throws Exception {
        mvc.perform(put("/api/prospectos/1/notas/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":2,\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.contenido").value("La nota no puede estar vacía"));
    }
}
