using System.Security.Claims;
using CaptaBot.Panel.Ayudas;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace CaptaBot.Panel.Tests;

/// <summary>API simulada para probar los controladores sin levantar la API de Java.</summary>
internal sealed class ApiFalsa : ICaptabotApi
{
    public Usuario? UsuarioDelLogin { get; set; }
    public Exception? ErrorDelLogin { get; set; }
    public Exception? ErrorAlCambiarEtapa { get; set; }

    public int Llamadas { get; private set; }
    public FiltroProspectos? UltimoFiltro { get; private set; }
    public (long Id, string Etapa, long UsuarioId)? UltimoCambioDeEtapa { get; private set; }
    public List<(long Id, bool Activo)> CambiosDeEstado { get; } = [];
    public List<UsuarioSolicitud> UsuariosCreados { get; } = [];

    public Task<Usuario?> IniciarSesionAsync(string correo, string contrasena, CancellationToken ct = default)
    {
        Llamadas++;
        if (ErrorDelLogin is not null) throw ErrorDelLogin;
        return Task.FromResult(UsuarioDelLogin);
    }

    public Task<PaginaResultado<Prospecto>> ListarProspectosAsync(FiltroProspectos filtro, CancellationToken ct = default)
    {
        Llamadas++;
        UltimoFiltro = filtro;
        return Task.FromResult(new PaginaResultado<Prospecto>([], 0, 20, 0, 0));
    }

    public Task<Prospecto?> ObtenerProspectoAsync(long id, CancellationToken ct = default) =>
        Task.FromResult<Prospecto?>(null);

    public Task<Prospecto> CambiarEtapaAsync(long id, string etapa, long usuarioId, CancellationToken ct = default)
    {
        Llamadas++;
        if (ErrorAlCambiarEtapa is not null) throw ErrorAlCambiarEtapa;
        UltimoCambioDeEtapa = (id, etapa, usuarioId);
        return Task.FromResult(new Prospecto(id, "María Quispe", "987654321", "Diseño web", 6000m, "ALTA", 100,
            "CALIENTE", etapa, "CHAT_WEB", null, DateTimeOffset.UtcNow));
    }

    public Task<IReadOnlyList<CambioEtapa>> HistorialAsync(long id, CancellationToken ct = default) =>
        Task.FromResult<IReadOnlyList<CambioEtapa>>([]);

    public Task<IReadOnlyList<Nota>> NotasAsync(long id, CancellationToken ct = default) =>
        Task.FromResult<IReadOnlyList<Nota>>([]);

    public Task<Nota> AgregarNotaAsync(long id, long usuarioId, string contenido, CancellationToken ct = default)
    {
        Llamadas++;
        return Task.FromResult(new Nota(1, contenido, usuarioId, "Ana", DateTimeOffset.UtcNow, null));
    }

    public Task<Nota> EditarNotaAsync(long id, long notaId, long usuarioId, string contenido, CancellationToken ct = default)
    {
        Llamadas++;
        return Task.FromResult(new Nota(notaId, contenido, usuarioId, "Ana", DateTimeOffset.UtcNow, DateTimeOffset.UtcNow));
    }

    public Task<IReadOnlyList<Usuario>> ListarUsuariosAsync(CancellationToken ct = default) =>
        Task.FromResult<IReadOnlyList<Usuario>>([]);

    public Task<Usuario?> ObtenerUsuarioAsync(long id, CancellationToken ct = default) =>
        Task.FromResult<Usuario?>(null);

    public Task<Usuario> CrearUsuarioAsync(UsuarioSolicitud solicitud, CancellationToken ct = default)
    {
        Llamadas++;
        UsuariosCreados.Add(solicitud);
        return Task.FromResult(new Usuario(3, solicitud.Nombre, solicitud.Correo, solicitud.Rol, true, DateTimeOffset.UtcNow));
    }

    public Task<Usuario> ActualizarUsuarioAsync(long id, UsuarioSolicitud solicitud, CancellationToken ct = default) =>
        Task.FromResult(new Usuario(id, solicitud.Nombre, solicitud.Correo, solicitud.Rol, true, DateTimeOffset.UtcNow));

    public Task<Usuario> CambiarEstadoUsuarioAsync(long id, bool activo, CancellationToken ct = default)
    {
        Llamadas++;
        CambiosDeEstado.Add((id, activo));
        return Task.FromResult(new Usuario(id, "Luis", "luis@captabot.pe", "VENDEDOR", activo, DateTimeOffset.UtcNow));
    }

    public Task<IReadOnlyList<ReporteSemanal>> ReporteSemanalAsync(int semanas, CancellationToken ct = default) =>
        Task.FromResult<IReadOnlyList<ReporteSemanal>>([]);

    public Task<IReadOnlyList<EtapaEmbudo>> EmbudoAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default) =>
        Task.FromResult<IReadOnlyList<EtapaEmbudo>>([]);

    public Task<TasaConversion> ConversionAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default) =>
        Task.FromResult(new TasaConversion(0, 0, 0m));
}

/// <summary>Responde a las solicitudes HTTP del cliente sin red y guarda lo que se envió.</summary>
internal sealed class ManejadorFalso(Func<HttpRequestMessage, HttpResponseMessage> responder) : HttpMessageHandler
{
    public List<HttpRequestMessage> Solicitudes { get; } = [];
    public List<string?> Cuerpos { get; } = [];

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage solicitud, CancellationToken ct)
    {
        Solicitudes.Add(solicitud);
        Cuerpos.Add(solicitud.Content is null ? null : await solicitud.Content.ReadAsStringAsync(ct));
        return responder(solicitud);
    }
}

internal sealed class TempDataFalso : ITempDataProvider
{
    public IDictionary<string, object> LoadTempData(HttpContext context) => new Dictionary<string, object>();

    public void SaveTempData(HttpContext context, IDictionary<string, object> values)
    {
    }
}

internal static class Preparar
{
    /// <summary>Simula un usuario con sesión iniciada en el controlador.</summary>
    public static T ConSesion<T>(T controlador, long usuarioId = 7, string rol = "VENDEDOR") where T : Controller
    {
        var usuario = new Usuario(usuarioId, "Ana Ruiz", "ana@captabot.pe", rol, true, null);
        var http = new DefaultHttpContext
        {
            User = new ClaimsPrincipal(new ClaimsIdentity(Sesion.Claims(usuario), "Prueba"))
        };
        controlador.ControllerContext = new ControllerContext { HttpContext = http };
        controlador.TempData = new TempDataDictionary(http, new TempDataFalso());
        return controlador;
    }
}
