package pe.captabot.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.HistorialEtapa;
import pe.captabot.api.modelo.NotaSeguimiento;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.repositorio.HistorialEtapaRepository;
import pe.captabot.api.repositorio.NotaSeguimientoRepository;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.repositorio.UsuarioRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del cambio de etapa (HU09) y de las notas de seguimiento (HU10). */
@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock private ProspectoRepository prospectos;
    @Mock private UsuarioRepository usuarios;
    @Mock private HistorialEtapaRepository historial;
    @Mock private NotaSeguimientoRepository notas;

    private SeguimientoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new SeguimientoService(prospectos, usuarios, historial, notas);
    }

    private static Prospecto prospecto(Etapa etapa) {
        Prospecto p = new Prospecto();
        p.setId(1L);
        p.setNombre("María Quispe");
        p.setEtapa(etapa);
        return p;
    }

    private static NotaSeguimiento nota(Long autorId) {
        NotaSeguimiento n = new NotaSeguimiento();
        n.setId(10L);
        n.setProspectoId(1L);
        n.setUsuarioId(autorId);
        n.setContenido("Pidió la cotización por correo");
        return n;
    }

    @Test
    void cambiaLaEtapaYGuardaQuienLoHizo() {
        Prospecto p = prospecto(Etapa.NUEVO);
        when(prospectos.findById(1L)).thenReturn(Optional.of(p));
        when(usuarios.existsById(2L)).thenReturn(true);
        when(prospectos.save(p)).thenReturn(p);

        Prospecto actualizado = servicio.cambiarEtapa(1L, Etapa.CONTACTADO, 2L);

        assertThat(actualizado.getEtapa()).isEqualTo(Etapa.CONTACTADO);
        ArgumentCaptor<HistorialEtapa> cambio = ArgumentCaptor.forClass(HistorialEtapa.class);
        verify(historial).save(cambio.capture());
        assertThat(cambio.getValue().getProspectoId()).isEqualTo(1L);
        assertThat(cambio.getValue().getEtapaAnterior()).isEqualTo(Etapa.NUEVO);
        assertThat(cambio.getValue().getEtapaNueva()).isEqualTo(Etapa.CONTACTADO);
        assertThat(cambio.getValue().getUsuarioId()).isEqualTo(2L);
    }

    @Test
    void noRegistraUnCambioALaMismaEtapa() {
        when(prospectos.findById(1L)).thenReturn(Optional.of(prospecto(Etapa.COTIZADO)));
        when(usuarios.existsById(2L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.cambiarEtapa(1L, Etapa.COTIZADO, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está en la etapa");
        verify(historial, never()).save(any());
    }

    @Test
    void rechazaUnUsuarioQueNoExiste() {
        when(prospectos.findById(1L)).thenReturn(Optional.of(prospecto(Etapa.NUEVO)));
        when(usuarios.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.cambiarEtapa(1L, Etapa.CONTACTADO, 9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuario 9");
    }

    @Test
    void agregaUnaNotaConSuAutor() {
        when(prospectos.findById(1L)).thenReturn(Optional.of(prospecto(Etapa.NUEVO)));
        when(usuarios.existsById(2L)).thenReturn(true);
        when(notas.save(any(NotaSeguimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        NotaSeguimiento nota = servicio.agregarNota(1L, 2L, "  Llamar el lunes a las 10  ");

        assertThat(nota.getContenido()).isEqualTo("Llamar el lunes a las 10");
        assertThat(nota.getUsuarioId()).isEqualTo(2L);
        assertThat(nota.getProspectoId()).isEqualTo(1L);
    }

    @Test
    void soloElAutorPuedeEditarSuNota() {
        when(notas.findByIdAndProspectoId(10L, 1L)).thenReturn(Optional.of(nota(2L)));

        assertThatThrownBy(() -> servicio.editarNota(1L, 10L, 3L, "Otro texto"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessage("Solo el autor puede editar la nota");
        verify(notas, never()).save(any());
    }

    @Test
    void elAutorEditaSuNotaYQuedaLaFechaDeEdicion() {
        when(notas.findByIdAndProspectoId(10L, 1L)).thenReturn(Optional.of(nota(2L)));
        when(notas.save(any(NotaSeguimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        NotaSeguimiento editada = servicio.editarNota(1L, 10L, 2L, "Envió la cotización firmada");

        assertThat(editada.getContenido()).isEqualTo("Envió la cotización firmada");
        assertThat(editada.getEditadoEn()).isNotNull();
    }

    @Test
    void avisaSiElProspectoNoExiste() {
        when(prospectos.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.notas(99L)).isInstanceOf(ProspectoNoEncontradoException.class);
    }
}
