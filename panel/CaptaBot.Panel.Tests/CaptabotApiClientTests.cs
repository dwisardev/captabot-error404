using System.Net;
using System.Text;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.Extensions.Options;

namespace CaptaBot.Panel.Tests;

/// <summary>El panel llama bien a la API y entiende sus respuestas y errores.</summary>
public class CaptabotApiClientTests
{
    private static CaptabotApiClient Cliente(ManejadorFalso manejador, string? clave = "clave-secreta") =>
        new(new HttpClient(manejador),
            Options.Create(new CaptabotApiOptions { UrlBase = "http://api.prueba", ClaveApi = clave }));

    private static HttpResponseMessage Json(HttpStatusCode estado, string json) =>
        new(estado) { Content = new StringContent(json, Encoding.UTF8, "application/json") };

    [Fact]
    public async Task ListarProspectos_ArmaLaConsultaYLeeLaPagina()
    {
        var manejador = new ManejadorFalso(_ => Json(HttpStatusCode.OK, """
            {"contenido":[{"id":1,"nombre":"María Quispe","telefono":"987654321","servicioInteres":"Diseño web",
              "presupuesto":6000.00,"urgencia":"ALTA","puntaje":100,"calificacion":"CALIENTE","etapa":"NUEVO",
              "canal":"CHAT_WEB","vendedorId":null,"creadoEn":"2026-11-17T15:30:00.123456Z"}],
             "pagina":1,"tamano":20,"totalElementos":21,"totalPaginas":2}
            """));
        var filtro = new FiltroProspectos { Calificacion = "CALIENTE", Desde = new DateOnly(2026, 11, 1), Pagina = 2 };

        var resultado = await Cliente(manejador).ListarProspectosAsync(filtro);

        var solicitud = Assert.Single(manejador.Solicitudes);
        Assert.Equal("http://api.prueba/api/prospectos?calificacion=CALIENTE&desde=2026-11-01&pagina=1&tamano=20",
            solicitud.RequestUri!.ToString());
        Assert.Equal("clave-secreta", solicitud.Headers.GetValues(CaptabotApiClient.CabeceraClave).Single());
        var prospecto = Assert.Single(resultado.Contenido);
        Assert.Equal("María Quispe", prospecto.Nombre);
        Assert.Equal(6000m, prospecto.Presupuesto);
        Assert.Equal(new DateTimeOffset(2026, 11, 17, 15, 30, 0, 123, TimeSpan.Zero).AddTicks(4560), prospecto.CreadoEn);
        Assert.Equal(2, resultado.TotalPaginas);
    }

    [Fact]
    public async Task SinClaveConfigurada_NoEnviaLaCabecera()
    {
        var manejador = new ManejadorFalso(_ => Json(HttpStatusCode.OK, "[]"));

        await Cliente(manejador, clave: null).ListarUsuariosAsync();

        Assert.False(manejador.Solicitudes[0].Headers.Contains(CaptabotApiClient.CabeceraClave));
    }

    [Fact]
    public async Task IniciarSesion_ConCredencialesIncorrectas_DevuelveNull()
    {
        var manejador = new ManejadorFalso(_ =>
            Json(HttpStatusCode.Unauthorized, """{"error":"Correo o contraseña incorrectos"}"""));

        var usuario = await Cliente(manejador).IniciarSesionAsync("ana@captabot.pe", "mala");

        Assert.Null(usuario);
        Assert.Equal(HttpMethod.Post, manejador.Solicitudes[0].Method);
        Assert.Contains("\"correo\":\"ana@captabot.pe\"", manejador.Cuerpos[0]);
    }

    [Fact]
    public async Task IniciarSesion_ConLaClaveDeApiMalConfigurada_LanzaError()
    {
        var manejador = new ManejadorFalso(_ =>
            Json(HttpStatusCode.Unauthorized, """{"error":"Falta la clave de la API o no es válida"}"""));

        var error = await Assert.ThrowsAsync<ApiException>(() => Cliente(manejador).IniciarSesionAsync("a@b.pe", "x"));

        Assert.Equal(HttpStatusCode.Unauthorized, error.Estado);
        Assert.False(error.EsErrorDelUsuario);
    }

    [Fact]
    public async Task CrearUsuario_ConCorreoRepetido_LanzaElMensajeDeLaApi()
    {
        var manejador = new ManejadorFalso(_ =>
            Json(HttpStatusCode.Conflict, """{"error":"Ya existe un usuario con el correo luis@captabot.pe"}"""));

        var error = await Assert.ThrowsAsync<ApiException>(() =>
            Cliente(manejador).CrearUsuarioAsync(new UsuarioSolicitud("Luis", "luis@captabot.pe", "Segura2026", "VENDEDOR")));

        Assert.Equal("Ya existe un usuario con el correo luis@captabot.pe", error.Message);
        Assert.True(error.EsErrorDelUsuario);
    }

    [Fact]
    public async Task CrearUsuario_ConDatosInvalidos_IncluyeElDetallePorCampo()
    {
        var manejador = new ManejadorFalso(_ => Json(HttpStatusCode.BadRequest,
            """{"error":"Datos inválidos","detalles":{"correo":"El correo no es válido"}}"""));

        var error = await Assert.ThrowsAsync<ApiException>(() =>
            Cliente(manejador).CrearUsuarioAsync(new UsuarioSolicitud("Luis", "luis", "Segura2026", "VENDEDOR")));

        Assert.Equal("El correo no es válido", error.Detalles["correo"]);
    }

    [Fact]
    public async Task ObtenerProspecto_QueNoExiste_DevuelveNull()
    {
        var manejador = new ManejadorFalso(_ =>
            Json(HttpStatusCode.NotFound, """{"error":"No existe el prospecto 99"}"""));

        Assert.Null(await Cliente(manejador).ObtenerProspectoAsync(99));
    }

    [Fact]
    public async Task CambiarEtapa_EnviaUnPatchConElUsuario()
    {
        var manejador = new ManejadorFalso(_ => Json(HttpStatusCode.OK, """
            {"id":1,"nombre":"María","telefono":"987654321","servicioInteres":"Web","presupuesto":10,
             "urgencia":"ALTA","puntaje":55,"calificacion":"TIBIO","etapa":"CONTACTADO","canal":"CHAT_WEB",
             "vendedorId":null,"creadoEn":"2026-11-17T10:00:00-05:00"}
            """));

        var prospecto = await Cliente(manejador).CambiarEtapaAsync(1, "CONTACTADO", 7);

        Assert.Equal(HttpMethod.Patch, manejador.Solicitudes[0].Method);
        Assert.Equal("/api/prospectos/1/etapa", manejador.Solicitudes[0].RequestUri!.AbsolutePath);
        Assert.Contains("\"usuarioId\":7", manejador.Cuerpos[0]);
        Assert.Equal("CONTACTADO", prospecto.Etapa);
    }

    [Fact]
    public async Task ReporteSemanal_LeeLasFechasDeCadaSemana()
    {
        var manejador = new ManejadorFalso(_ => Json(HttpStatusCode.OK,
            """[{"semana":"2026-11-16","total":3,"calientes":1,"tibios":1,"frios":1}]"""));

        var semanas = await Cliente(manejador).ReporteSemanalAsync(8);

        Assert.Equal("semanas=8", manejador.Solicitudes[0].RequestUri!.Query.TrimStart('?'));
        Assert.Equal(new DateOnly(2026, 11, 16), Assert.Single(semanas).Semana);
    }

    [Fact]
    public void ConConsulta_OmiteLosFiltrosVaciosYCodificaLosValores()
    {
        var ruta = CaptabotApiClient.ConConsulta("api/x", ("a", null), ("b", " "), ("c", "uno dos"));

        Assert.Equal("api/x?c=uno%20dos", ruta);
    }
}
