package pe.captabot.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.web.ProspectoRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas del registro (HU05) y de la lista de prospectos (HU08). */
@ExtendWith(MockitoExtension.class)
class ProspectoServiceTest {

    @Mock
    private ProspectoRepository repositorio;

    private ProspectoService servicio;
    private final List<Object> eventos = new java.util.ArrayList<>();

    @BeforeEach
    void preparar() {
        servicio = new ProspectoService(repositorio, new CalificacionService(), eventos::add);
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
        assertThat(registrado.getTelegramChatId()).isNull();
        assertThat(eventos).containsExactly(new ProspectoRegistrado(registrado));
    }

    @Test
    void guardaElChatDeTelegramDelCliente() {
        when(repositorio.save(any(Prospecto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProspectoRequest solicitud = new ProspectoRequest(
                "Rosa Vega", "987654321", "Diseño web", new BigDecimal("3000"), "media");

        Prospecto registrado = servicio.registrar(solicitud, "TELEGRAM", "77");

        assertThat(registrado.getCanal()).isEqualTo("TELEGRAM");
        assertThat(registrado.getTelegramChatId()).isEqualTo(77L);
        assertThat(registrado.getWhatsappId()).isNull();
        assertThat(eventos).hasSize(1);
    }

    @Test
    void guardaElNumeroDeWhatsappDelCliente() {
        when(repositorio.save(any(Prospecto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProspectoRequest solicitud = new ProspectoRequest(
                "Rosa Vega", "955777888", "Diseño web", new BigDecimal("3000"), "media");

        Prospecto registrado = servicio.registrar(solicitud, "WHATSAPP", "51955777888");

        assertThat(registrado.getCanal()).isEqualTo("WHATSAPP");
        assertThat(registrado.getWhatsappId()).isEqualTo("51955777888");
        assertThat(registrado.getTelegramChatId()).isNull();
    }

    @Test
    void resumenCuentaPorCanalYCalificacionConCerosParaLosQueFaltan() {
        when(repositorio.contarPorCanal()).thenReturn(List.of(new Object[]{"CHAT_WEB", 7L}, new Object[]{"WHATSAPP", 2L}));
        when(repositorio.contarPorCalificacion()).thenReturn(List.of(new Object[]{Calificacion.CALIENTE, 4L},
                new Object[]{Calificacion.FRIO, 5L}));
        when(repositorio.countByCalificacionAndEtapa(Calificacion.CALIENTE, Etapa.NUEVO)).thenReturn(1L);
        when(repositorio.countByCreadoEnGreaterThanEqual(any())).thenReturn(3L);

        ProspectoService.ResumenProspectos resumen = servicio.resumen();

        assertThat(resumen.total()).isEqualTo(9);
        assertThat(resumen.porCanal()).containsExactly(java.util.Map.entry("CHAT_WEB", 7L),
                java.util.Map.entry("TELEGRAM", 0L), java.util.Map.entry("WHATSAPP", 2L));
        assertThat(resumen.porCalificacion()).containsEntry("TIBIO", 0L).containsEntry("CALIENTE", 4L);
        assertThat(resumen.calientesSinContactar()).isEqualTo(1);
        assertThat(resumen.nuevosHoy()).isEqualTo(3);
    }

    @Test
    void recientesSinReferenciaSoloDevuelveElUltimoId() {
        when(repositorio.ultimoId()).thenReturn(42L);

        ProspectoService.Recientes recientes = servicio.recientes(null);

        assertThat(recientes.ultimoId()).isEqualTo(42L);
        assertThat(recientes.prospectos()).isEmpty();
        verify(repositorio, never()).findTop20ByIdGreaterThanOrderByIdDesc(any());
    }

    @Test
    void noGuardaSiLaUrgenciaNoEsValida() {
        ProspectoRequest solicitud = new ProspectoRequest(
                "Jorge", "912345678", "Mantenimiento", new BigDecimal("100"), "mañana");

        assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(IllegalArgumentException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void listaPaginasDe20ConLosDeMayorPuntajePrimero() {
        when(repositorio.findAll(ArgumentMatchers.<Specification<Prospecto>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Prospecto())));
        FiltroProspectos filtro = new FiltroProspectos(Calificacion.CALIENTE, Etapa.NUEVO,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        Page<Prospecto> pagina = servicio.listar(filtro, 0, ProspectoService.TAMANO_PAGINA);

        assertThat(pagina.getContent()).hasSize(1);
        ArgumentCaptor<Pageable> solicitada = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(ArgumentMatchers.<Specification<Prospecto>>any(), solicitada.capture());
        assertThat(solicitada.getValue().getPageSize()).isEqualTo(20);
        assertThat(solicitada.getValue().getSort().getOrderFor("puntaje").isDescending()).isTrue();
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        FiltroProspectos filtro = new FiltroProspectos(null, null, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> servicio.listar(filtro, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha inicial");
        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaUnTamanoDePaginaFueraDeRango() {
        assertThatThrownBy(() -> servicio.listar(FiltroProspectos.sinFiltros(), 0, 500))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void avisaCuandoElProspectoNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(ProspectoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}
