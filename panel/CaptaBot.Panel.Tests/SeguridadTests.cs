using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;

namespace CaptaBot.Panel.Tests;

/// <summary>HU06: ninguna página del panel se abre sin haber iniciado sesión.</summary>
public class SeguridadTests(WebApplicationFactory<Program> fabrica) : IClassFixture<WebApplicationFactory<Program>>
{
    private HttpClient Cliente() =>
        fabrica.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });

    [Theory]
    [InlineData("/")]
    [InlineData("/Prospectos")]
    [InlineData("/Prospectos/Detalle/1")]
    [InlineData("/Reportes")]
    [InlineData("/Reportes/Excel")]
    [InlineData("/Usuarios")]
    public async Task SinSesion_RedirigeAlInicioDeSesion(string ruta)
    {
        var respuesta = await Cliente().GetAsync(ruta);

        Assert.Equal(HttpStatusCode.Redirect, respuesta.StatusCode);
        Assert.Contains("/Cuenta/Login", respuesta.Headers.Location!.ToString());
    }

    [Fact]
    public async Task LaPaginaDeInicioDeSesionEstaDisponible()
    {
        var respuesta = await Cliente().GetAsync("/Cuenta/Login");

        respuesta.EnsureSuccessStatusCode();
        var html = await respuesta.Content.ReadAsStringAsync();
        Assert.Contains("Iniciar sesión", html);
        Assert.Contains("__RequestVerificationToken", html);
    }

    [Fact]
    public async Task UnPostSinTokenAntifalsificacionEsRechazado()
    {
        var contenido = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            ["Correo"] = "admin@captabot.pe",
            ["Contrasena"] = "Demo2026!"
        });

        var respuesta = await Cliente().PostAsync("/Cuenta/Login", contenido);

        Assert.Equal(HttpStatusCode.BadRequest, respuesta.StatusCode);
    }
}
