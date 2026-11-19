package pe.captabot.api.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;
import pe.captabot.api.modelo.AvisoTelegram;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.AvisoTelegramRepository;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.servicio.ProspectoRegistrado;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.whatsapp.WhatsAppApi;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU13: avisos al equipo de ventas, recordatorios y seguimiento al cliente por Telegram. */
class AvisosServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-18T20:00:00Z"), ZoneId.of("America/Lima"));
    private static final OffsetDateTime AHORA = OffsetDateTime.now(RELOJ);

    private TelegramBotTest.TelegramFalso telegram;
    private WhatsAppApi whatsapp;
    private AvisoTelegramRepository vendedores;
    private ProspectoRepository prospectos;
    private AvisosService avisos;

    @BeforeEach
    void preparar() {
        telegram = new TelegramBotTest.TelegramFalso();
        whatsapp = mock(WhatsAppApi.class);
        vendedores = mock(AvisoTelegramRepository.class);
        prospectos = mock(ProspectoRepository.class);
        avisos = crear(telegram, "clave-123");
    }

    private AvisosService crear(TelegramApi api, String codigo) {
        return crear(api, codigo, 1440);
    }

    private AvisosService crear(TelegramApi api, String codigo, long minutosSeguimiento) {
        return new AvisosService(api, whatsapp, vendedores, prospectos, codigo, 120, minutosSeguimiento, RELOJ,
                Runnable::run);
    }

    private static Prospecto prospecto(String nombre, Calificacion calificacion, int puntaje, int minutosAtras) {
        Prospecto p = new Prospecto();
        p.setId(5L);
        p.setNombre(nombre);
        p.setTelefono("945123678");
        p.setServicioInteres("Diseño web");
        p.setPresupuesto(new BigDecimal("3500"));
        p.setUrgencia(Urgencia.ALTA);
        p.setPuntaje(puntaje);
        p.setCalificacion(calificacion);
        p.setEtapa(Etapa.NUEVO);
        p.setCanal("CHAT_WEB");
        p.setCreadoEn(AHORA.minusMinutes(minutosAtras));
        return p;
    }

    // ------------------------------------------------------------------ vínculo

    @Test
    void vinculaAlVendedorConElCodigoCorrecto() {
        String respuesta = avisos.vincular(9L, "Ana", " clave-123 ");

        ArgumentCaptor<AvisoTelegram> guardado = ArgumentCaptor.forClass(AvisoTelegram.class);
        verify(vendedores).save(guardado.capture());
        assertThat(guardado.getValue().getChatId()).isEqualTo(9L);
        assertThat(guardado.getValue().getNombre()).isEqualTo("Ana");
        assertThat(respuesta).startsWith("Listo, Ana.").contains("después de 2 h", "/salir");
    }

    @Test
    void rechazaUnCodigoIncorrecto() {
        String respuesta = avisos.vincular(9L, "Ana", "otra");

        assertThat(respuesta).contains("no es correcto");
        verify(vendedores, never()).save(any());
    }

    @Test
    void sinCodigoEnElEnvLosAvisosEstanApagados() {
        AvisosService sinCodigo = crear(telegram, "");

        assertThat(sinCodigo.vincular(9L, "Ana", "clave-123")).contains("TELEGRAM_CODIGO_VENTAS");
        assertThat(sinCodigo.avisosActivos()).isFalse();
        assertThat(avisos.avisosActivos()).isTrue();
        verify(vendedores, never()).save(any());
    }

    @Test
    void salirQuitaAlVendedor() {
        when(vendedores.existsById(9L)).thenReturn(true);

        assertThat(avisos.desvincular(9L)).contains("ya no recibirás avisos");
        verify(vendedores).deleteById(9L);
    }

    // ------------------------------------------------------------------ 1. aviso inmediato

    @Test
    void avisaATodoElEquipoCuandoLlegaUnProspecto() {
        when(vendedores.findAll()).thenReturn(List.of(new AvisoTelegram(9L, "Ana"), new AvisoTelegram(10L, "Luis")));

        avisos.alRegistrar(new ProspectoRegistrado(prospecto("valeria ríos", Calificacion.CALIENTE, 85, 0)));

        assertThat(telegram.enviados).extracting(TelegramBotTest.TelegramFalso.Enviado::chatId).containsExactly(9L, 10L);
        assertThat(telegram.ultimo().texto()).startsWith("🔥 Nuevo prospecto caliente (85 puntos)")
                .contains("Valeria Ríos", "945 123 678", "S/ 3,500.00", "alta (esta semana)", "Chat web");
        assertThat(telegram.ultimo().teclado().toString()).contains("https://wa.me/51945123678");
        verify(prospectos).marcarAvisoEnviado(5L, AHORA);
    }

    @Test
    void sinVendedoresNoMarcaElAviso() {
        when(vendedores.findAll()).thenReturn(List.of());

        avisos.alRegistrar(new ProspectoRegistrado(prospecto("Ana", Calificacion.CALIENTE, 85, 0)));

        assertThat(telegram.enviados).isEmpty();
        verify(prospectos, never()).marcarAvisoEnviado(any(), any());
    }

    @Test
    void sinTelegramNoAvisaNiRevisa() {
        AvisosService apagado = crear(new TelegramApi("", RestClient.builder()), "clave-123");

        apagado.alRegistrar(new ProspectoRegistrado(prospecto("Ana", Calificacion.CALIENTE, 85, 0)));
        apagado.revisar();

        verifyNoInteractions(vendedores, prospectos);
    }

    // ------------------------------------------------------------------ 2. recordatorio

    @Test
    void recuerdaUnaVezLosCalientesQueSiguenSinContactar() {
        Prospecto pendiente = prospecto("Valeria Ríos", Calificacion.CALIENTE, 85, 150);
        OffsetDateTime hasta = AHORA.minusMinutes(120);
        when(vendedores.findAll()).thenReturn(List.of(new AvisoTelegram(9L, "Ana")));
        when(prospectos.pendientesDeRecordatorio(Etapa.NUEVO, AvisosService.A_RECORDAR,
                hasta.minus(AvisosService.VENTANA), hasta)).thenReturn(List.of(pendiente));

        avisos.recordarPendientes();

        assertThat(telegram.ultimo().chatId()).isEqualTo(9L);
        assertThat(telegram.ultimo().texto()).startsWith("⏰ Recordatorio: Valeria Ríos (caliente, 85 puntos)")
                .contains("desde hace 2 h 30 min");
        verify(prospectos).marcarRecordatorio(5L, AHORA);
    }

    @Test
    void sinVendedoresNoMarcaLosRecordatorios() {
        when(vendedores.findAll()).thenReturn(List.of());

        avisos.recordarPendientes();

        verify(prospectos, never()).pendientesDeRecordatorio(any(), any(), any(), any());
        assertThat(telegram.enviados).isEmpty();
    }

    // ------------------------------------------------------------------ 3. seguimiento al cliente

    @Test
    void escribeAlClienteQueLlegoPorTelegram() {
        Prospecto cliente = prospecto("rosa vega", Calificacion.TIBIO, 65, 1500);
        cliente.setCanal("TELEGRAM");
        cliente.setTelegramChatId(77L);
        OffsetDateTime hasta = AHORA.minusMinutes(1440);
        when(prospectos.pendientesDeSeguimiento(AvisosService.CERRADAS, hasta.minus(AvisosService.VENTANA), hasta))
                .thenReturn(List.of(cliente));

        avisos.seguirClientes();

        assertThat(telegram.ultimo().chatId()).isEqualTo(77L);
        assertThat(telegram.ultimo().texto()).startsWith("Hola, Rosa 👋").contains("Diseño web", "945 123 678");
        assertThat(telegram.ultimo().teclado().toString()).contains("Quiero cotizar");
        verify(prospectos).marcarSeguimiento(5L, AHORA);
        verify(whatsapp, never()).enviar(any(), any(), any());
    }

    @Test
    void escribeAlClienteQueLlegoPorWhatsappDentroDeLas24Horas() {
        AvisosService rapido = crear(telegram, "clave-123", 2);
        Prospecto cliente = prospecto("rosa vega", Calificacion.TIBIO, 65, 3);
        cliente.setCanal("WHATSAPP");
        cliente.setWhatsappId("51955777888");
        when(whatsapp.estaConfigurado()).thenReturn(true);
        OffsetDateTime hasta = AHORA.minusMinutes(2);
        when(prospectos.pendientesDeSeguimiento(AvisosService.CERRADAS, hasta.minus(AvisosService.VENTANA), hasta))
                .thenReturn(List.of(cliente));

        rapido.seguirClientes();

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(whatsapp).enviar(eq("51955777888"), texto.capture(), eq(ConversacionService.MENU));
        assertThat(texto.getValue()).startsWith("Hola, Rosa 👋");
        verify(prospectos).marcarSeguimiento(5L, AHORA);
        assertThat(telegram.enviados).isEmpty();
    }

    @Test
    void porWhatsappNoSigueDespuesDe23HorasPorqueRequierePlantillaDePago() {
        Prospecto cliente = prospecto("rosa vega", Calificacion.TIBIO, 65, 1500);
        cliente.setCanal("WHATSAPP");
        cliente.setWhatsappId("51955777888");
        when(whatsapp.estaConfigurado()).thenReturn(true);
        OffsetDateTime hasta = AHORA.minusMinutes(1440);
        when(prospectos.pendientesDeSeguimiento(AvisosService.CERRADAS, hasta.minus(AvisosService.VENTANA), hasta))
                .thenReturn(List.of(cliente));

        avisos.seguirClientes();

        assertThat(avisos.seguimientoWhatsappDisponible()).isFalse();
        verify(whatsapp, never()).enviar(any(), any(), any());
        verify(prospectos, never()).marcarSeguimiento(any(), any());
    }

    // ------------------------------------------------------------------ textos

    @Test
    void muestraLosTiemposYNombresDeFormaLegible() {
        assertThat(AvisosService.duracion(45)).isEqualTo("45 min");
        assertThat(AvisosService.duracion(120)).isEqualTo("2 h");
        assertThat(AvisosService.duracion(150)).isEqualTo("2 h 30 min");
        assertThat(AvisosService.duracion(1440)).isEqualTo("1 día");
        assertThat(AvisosService.duracion(2880)).isEqualTo("2 días");
        assertThat(AvisosService.nombre("wilder  TUDELA")).isEqualTo("Wilder Tudela");
        assertThat(AvisosService.telefono("919697458")).isEqualTo("919 697 458");
    }

    @Test
    void siLosMinutosDelEnvEstanVaciosUsaElValorPorDefecto() {
        assertThat(AvisosService.minutos("", 120)).isEqualTo(120);
        assertThat(AvisosService.minutos(" 5 ", 120)).isEqualTo(5);
        assertThat(AvisosService.minutos("dos", 1440)).isEqualTo(1440);
    }
}
