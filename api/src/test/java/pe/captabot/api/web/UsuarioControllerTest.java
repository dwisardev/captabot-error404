package pe.captabot.api.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.captabot.api.modelo.Rol;
import pe.captabot.api.modelo.Usuario;
import pe.captabot.api.servicio.ConflictoException;
import pe.captabot.api.servicio.CredencialesInvalidasException;
import pe.captabot.api.servicio.UsuarioService;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del inicio de sesión (HU06) y del registro de usuarios (HU07). */
class UsuarioControllerTest {

    private UsuarioService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(UsuarioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(servicio), new UsuarioController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    private static Usuario usuario(Long id, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Administrador");
        u.setCorreo("admin@captabot.pe");
        u.setRol(rol);
        u.setActivo(activo);
        u.setContrasenaHash("$2a$10$hash");
        u.setCreadoEn(OffsetDateTime.now());
        return u;
    }

    @Test
    void loginCorrectoDevuelveElUsuarioSinSuContrasena() throws Exception {
        when(servicio.autenticar("admin@captabot.pe", "Demo2026!")).thenReturn(usuario(1L, Rol.ADMINISTRADOR, true));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@captabot.pe\",\"contrasena\":\"Demo2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void loginIncorrectoDevuelve401ConMensaje() throws Exception {
        when(servicio.autenticar(any(), any())).thenThrow(new CredencialesInvalidasException());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@captabot.pe\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos"));
    }

    @Test
    void creaUnUsuario() throws Exception {
        when(servicio.crear(any())).thenReturn(usuario(3L, Rol.VENDEDOR, true));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Luis Pérez","correo":"luis@captabot.pe",
                                 "contrasena":"Segura2026","rol":"VENDEDOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/usuarios/3"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"));
    }

    @Test
    void correoRepetidoDevuelve409() throws Exception {
        when(servicio.crear(any())).thenThrow(new ConflictoException("Ya existe un usuario con el correo luis@captabot.pe"));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Luis Pérez","correo":"luis@captabot.pe",
                                 "contrasena":"Segura2026","rol":"VENDEDOR"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un usuario con el correo luis@captabot.pe"));
    }

    @Test
    void correoConFormatoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis\",\"correo\":\"no-es-un-correo\",\"contrasena\":\"Segura2026\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").value("El correo no es válido"));
    }

    @Test
    void desactivaUnUsuario() throws Exception {
        when(servicio.cambiarEstado(3L, false)).thenReturn(usuario(3L, Rol.VENDEDOR, false));

        mvc.perform(patch("/api/usuarios/3/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
