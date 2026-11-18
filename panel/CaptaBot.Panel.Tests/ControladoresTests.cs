using System.Net;
using CaptaBot.Panel.Controllers;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Mvc;

namespace CaptaBot.Panel.Tests;

public class CuentaControllerTests
{
    [Fact]
    public async Task Login_ConCredencialesIncorrectas_MuestraElErrorYLimpiaLaContrasena()
    {
        var api = new ApiFalsa { UsuarioDelLogin = null };
        var controlador = new CuentaController(api);

        var resultado = await controlador.Login(new LoginViewModel { Correo = "ana@captabot.pe", Contrasena = "mala" }, default);

        var vista = Assert.IsType<ViewResult>(resultado);
        var modelo = Assert.IsType<LoginViewModel>(vista.Model);
        Assert.Equal("", modelo.Contrasena);
        Assert.Equal(CuentaController.MensajeCredenciales, controlador.ModelState[string.Empty]!.Errors[0].ErrorMessage);
    }

    [Fact]
    public async Task Login_SinConexionConLaApi_LoExplica()
    {
        var api = new ApiFalsa { ErrorDelLogin = new HttpRequestException("sin conexión") };
        var controlador = new CuentaController(api);

        await controlador.Login(new LoginViewModel { Correo = "ana@captabot.pe", Contrasena = "Demo2026!" }, default);

        Assert.Equal(CuentaController.MensajeSinApi, controlador.ModelState[string.Empty]!.Errors[0].ErrorMessage);
    }

    [Fact]
    public async Task Login_ConDatosIncompletos_NoLlamaALaApi()
    {
        var api = new ApiFalsa();
        var controlador = new CuentaController(api);
        controlador.ModelState.AddModelError("Correo", "Ingresa tu correo");

        await controlador.Login(new LoginViewModel(), default);

        Assert.Equal(0, api.Llamadas);
    }
}

public class ProspectosControllerTests
{
    [Fact]
    public async Task Index_IgnoraFiltrosDesconocidosYEmpiezaEnLaPagina1()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new ProspectosController(api));

        await controlador.Index(new FiltroProspectos { Calificacion = "MUY_CALIENTE", Etapa = "NUEVO", Pagina = 0 }, default);

        Assert.Null(api.UltimoFiltro!.Calificacion);
        Assert.Equal("NUEVO", api.UltimoFiltro.Etapa);
        Assert.Equal(1, api.UltimoFiltro.Pagina);
    }

    [Fact]
    public async Task Index_ConRangoDeFechasInvertido_AvisaSinConsultarLaApi()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new ProspectosController(api));

        var resultado = await controlador.Index(
            new FiltroProspectos { Desde = new DateOnly(2026, 11, 30), Hasta = new DateOnly(2026, 11, 1) }, default);

        Assert.IsType<ViewResult>(resultado);
        Assert.False(controlador.ModelState.IsValid);
        Assert.Equal(0, api.Llamadas);
    }

    [Fact]
    public async Task CambiarEtapa_GuardaElUsuarioDeLaSesion()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new ProspectosController(api), usuarioId: 7);

        var resultado = await controlador.CambiarEtapa(1, "CONTACTADO", default);

        Assert.Equal((1L, "CONTACTADO", 7L), api.UltimoCambioDeEtapa!.Value);
        Assert.NotNull(controlador.TempData[ProspectosController.ClaveExito]);
        var redireccion = Assert.IsType<RedirectToActionResult>(resultado);
        Assert.Equal(nameof(ProspectosController.Detalle), redireccion.ActionName);
    }

    [Fact]
    public async Task CambiarEtapa_MuestraElErrorQueDevuelveLaApi()
    {
        var api = new ApiFalsa
        {
            ErrorAlCambiarEtapa = new ApiException(HttpStatusCode.BadRequest,
                "El prospecto ya está en la etapa CONTACTADO", new Dictionary<string, string>())
        };
        var controlador = Preparar.ConSesion(new ProspectosController(api));

        await controlador.CambiarEtapa(1, "CONTACTADO", default);

        Assert.Equal("El prospecto ya está en la etapa CONTACTADO", controlador.TempData[ProspectosController.ClaveError]);
    }

    [Fact]
    public async Task CambiarEtapa_ConUnaEtapaInventada_NoLlamaALaApi()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new ProspectosController(api));

        await controlador.CambiarEtapa(1, "ARCHIVADO", default);

        Assert.Equal(0, api.Llamadas);
    }

    [Fact]
    public async Task AgregarNota_Vacia_NoSeGuarda()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new ProspectosController(api));

        await controlador.AgregarNota(1, "   ", default);

        Assert.Equal(0, api.Llamadas);
        Assert.NotNull(controlador.TempData[ProspectosController.ClaveError]);
    }
}

public class UsuariosControllerTests
{
    [Fact]
    public async Task CambiarEstado_ElAdministradorNoPuedeDesactivarseASiMismo()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new UsuariosController(api), usuarioId: 1, rol: "ADMINISTRADOR");

        await controlador.CambiarEstado(1, activo: false, default);

        Assert.Empty(api.CambiosDeEstado);
        Assert.Equal("No puedes desactivar tu propio usuario.", controlador.TempData[ProspectosController.ClaveError]);
    }

    [Fact]
    public async Task CambiarEstado_DesactivaAOtroUsuario()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new UsuariosController(api), usuarioId: 1, rol: "ADMINISTRADOR");

        await controlador.CambiarEstado(3, activo: false, default);

        Assert.Equal((3L, false), Assert.Single(api.CambiosDeEstado));
    }

    [Fact]
    public async Task Nuevo_SinContrasena_PideCompletarla()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new UsuariosController(api), usuarioId: 1, rol: "ADMINISTRADOR");
        var formulario = new UsuarioFormulario { Nombre = "Luis", Correo = "luis@captabot.pe", Rol = "VENDEDOR" };

        var resultado = await controlador.Nuevo(formulario, default);

        Assert.IsType<ViewResult>(resultado);
        Assert.True(controlador.ModelState.ContainsKey(nameof(UsuarioFormulario.Contrasena)));
        Assert.Empty(api.UsuariosCreados);
    }

    [Fact]
    public async Task Nuevo_ConDatosCompletos_CreaElUsuario()
    {
        var api = new ApiFalsa();
        var controlador = Preparar.ConSesion(new UsuariosController(api), usuarioId: 1, rol: "ADMINISTRADOR");
        var formulario = new UsuarioFormulario
        {
            Nombre = " Luis Pérez ", Correo = "luis@captabot.pe", Contrasena = "Segura2026", Rol = "VENDEDOR"
        };

        var resultado = await controlador.Nuevo(formulario, default);

        Assert.IsType<RedirectToActionResult>(resultado);
        Assert.Equal("Luis Pérez", Assert.Single(api.UsuariosCreados).Nombre);
    }
}

public class ReportesControllerTests
{
    [Fact]
    public void Hojas_IncluyeLosTresReportesConSusEncabezados()
    {
        var modelo = new ReportesViewModel(
            [new ReporteSemanal(new DateOnly(2026, 11, 16), 3, 1, 1, 1)],
            [new EtapaEmbudo("CERRADO_GANADO", 1)],
            new TasaConversion(10, 1, 10.00m),
            new DateOnly(2026, 11, 1), new DateOnly(2026, 11, 30), true);

        var hojas = ReportesController.Hojas(modelo);

        Assert.Equal(new[] { "Por semana", "Embudo", "Conversión" }, hojas.Select(h => h.Nombre));
        Assert.Equal("16/11/2026", hojas[0].Filas[1][0]);
        Assert.Equal("Cerrado – ganado", hojas[1].Filas[1][0]);
        Assert.Equal("Del 01/11/2026 al 30/11/2026", hojas[2].Filas[1][0]);
        Assert.Equal(10.00m, hojas[2].Filas[1][3]);
    }

    [Fact]
    public void Porcentaje_CalculaElAltoDeLaBarra()
    {
        Assert.Equal(50, ReportesViewModel.Porcentaje(2, 4));
        Assert.Equal(100, ReportesViewModel.Porcentaje(4, 4));
    }
}
