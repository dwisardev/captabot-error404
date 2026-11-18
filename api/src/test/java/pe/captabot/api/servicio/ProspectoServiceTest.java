package pe.captabot.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.web.ProspectoRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del registro y la consulta de prospectos (HU05). */
@ExtendWith(MockitoExtension.class)
class ProspectoServiceTest {

    @Mock
    private ProspectoRepository repositorio;

    private ProspectoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProspectoService(repositorio, new CalificacionService());
    }

    @Test
    void registraElProspectoCalificadoYEnEtapaNuevo() {
        when(repositorio.save(any(Prospecto.class))).thenAnswer(inv -> {
            Prospecto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        ProspectoRequest solicitud = new ProspectoRequest(
                "  María Quispe ", "987654321", " Diseño de página web ", new BigDecimal("6000"), "alta");

        Prospecto registrado = servicio.registrar(solicitud);

        assertThat(registrado.getId()).isEqualTo(1L);
        assertThat(registrado.getNombre()).isEqualTo("María Quispe");
        assertThat(registrado.getServicioInteres()).isEqualTo("Diseño de página web");
        assertThat(registrado.getUrgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(registrado.getPuntaje()).isEqualTo(100);
        assertThat(registrado.getCalificacion()).isEqualTo(Calificacion.CALIENTE);
        assertThat(registrado.getEtapa()).isEqualTo(Etapa.NUEVO);
        assertThat(registrado.getCanal()).isEqualTo("CHAT_WEB");
    }

    @Test
    void noGuardaSiLaUrgenciaNoEsValida() {
        ProspectoRequest solicitud = new ProspectoRequest(
                "Jorge", "912345678", "Mantenimiento", new BigDecimal("100"), "mañana");

        assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void filtraPorCalificacionYEtapa() {
        when(repositorio.findByCalificacionAndEtapaOrderByPuntajeDescCreadoEnDesc(Calificacion.CALIENTE, Etapa.NUEVO))
                .thenReturn(List.of(new Prospecto()));

        assertThat(servicio.listar(Calificacion.CALIENTE, Etapa.NUEVO)).hasSize(1);
    }

    @Test
    void avisaCuandoElProspectoNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(ProspectoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
