package pe.captabot.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.captabot.api.modelo.Rol;
import pe.captabot.api.modelo.Usuario;
import pe.captabot.api.repositorio.UsuarioRepository;
import pe.captabot.api.web.UsuarioRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del inicio de sesión (HU06) y de la gestión de usuarios (HU07). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repositorio;

    /** Costo bajo para que las pruebas sean rápidas. */
    private final PasswordEncoder cifrador = new BCryptPasswordEncoder(4);

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(repositorio, cifrador);
    }

    private Usuario usuario(String contrasena, boolean activo) {
        Usuario u = new Usuario();
        u.setId(5L);
        u.setNombre("Ana Ruiz");
        u.setCorreo("ana@empresa.pe");
        u.setRol(Rol.VENDEDOR);
        u.setActivo(activo);
        u.setContrasenaHash(cifrador.encode(contrasena));
        return u;
    }

    @Test
    void creaElUsuarioConCorreoNormalizadoYContrasenaCifrada() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = servicio.crear(new UsuarioRequest(" Ana Ruiz ", " Ana@Empresa.PE ", "Segura2026", "vendedor"));

        assertThat(creado.getNombre()).isEqualTo("Ana Ruiz");
        assertThat(creado.getCorreo()).isEqualTo("ana@empresa.pe");
        assertThat(creado.getRol()).isEqualTo(Rol.VENDEDOR);
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getContrasenaHash()).isNotEqualTo("Segura2026");
        assertThat(cifrador.matches("Segura2026", creado.getContrasenaHash())).isTrue();
    }

    @Test
    void noPermiteDosUsuariosConElMismoCorreo() {
        when(repositorio.existsByCorreoIgnoreCase("ana@empresa.pe")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new UsuarioRequest("Ana", "ANA@empresa.pe", "Segura2026", "VENDEDOR")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ana@empresa.pe");
        verify(repositorio, never()).save(any());
    }

    @Test
    void exigeUnaContrasenaDeAlMenos8Caracteres() {
        assertThatThrownBy(() -> servicio.crear(new UsuarioRequest("Ana", "ana@empresa.pe", "corta", "VENDEDOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaUnRolDesconocido() {
        assertThatThrownBy(() -> servicio.crear(new UsuarioRequest("Ana", "ana@empresa.pe", "Segura2026", "gerente")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMINISTRADOR o VENDEDOR");
    }

    @Test
    void alEditarSinContrasenaConservaLaAnterior() {
        Usuario existente = usuario("Segura2026", true);
        String hashAnterior = existente.getContrasenaHash();
        when(repositorio.findById(5L)).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario editado = servicio.actualizar(5L, new UsuarioRequest("Ana María Ruiz", "ana@empresa.pe", null, "ADMINISTRADOR"));

        assertThat(editado.getNombre()).isEqualTo("Ana María Ruiz");
        assertThat(editado.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(editado.getContrasenaHash()).isEqualTo(hashAnterior);
    }

    @Test
    void alEditarNoPermiteUsarElCorreoDeOtroUsuario() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(usuario("Segura2026", true)));
        when(repositorio.existsByCorreoIgnoreCaseAndIdNot("luis@empresa.pe", 5L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(5L, new UsuarioRequest("Ana", "luis@empresa.pe", null, "VENDEDOR")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void iniciaSesionConLaContrasenaCorrecta() {
        when(repositorio.findByCorreoIgnoreCase("ana@empresa.pe")).thenReturn(Optional.of(usuario("Segura2026", true)));

        Usuario usuario = servicio.autenticar(" ANA@empresa.pe ", "Segura2026");

        assertThat(usuario.getId()).isEqualTo(5L);
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        when(repositorio.findByCorreoIgnoreCase("ana@empresa.pe")).thenReturn(Optional.of(usuario("Segura2026", true)));

        assertThatThrownBy(() -> servicio.autenticar("ana@empresa.pe", "otra-clave"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void rechazaUnCorreoQueNoExiste() {
        when(repositorio.findByCorreoIgnoreCase("nadie@empresa.pe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.autenticar("nadie@empresa.pe", "Segura2026"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIniciarSesion() {
        when(repositorio.findByCorreoIgnoreCase("ana@empresa.pe")).thenReturn(Optional.of(usuario("Segura2026", false)));

        assertThatThrownBy(() -> servicio.autenticar("ana@empresa.pe", "Segura2026"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void desactivaUnUsuario() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(usuario("Segura2026", true)));
        when(repositorio.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.cambiarEstado(5L, false).isActivo()).isFalse();
    }
}
