using System.Globalization;
using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using CaptaBot.Panel.Modelos;
using Microsoft.Extensions.Options;

namespace CaptaBot.Panel.Servicios;

/// <summary>Cliente HTTP de la API de CaptaBot. Envía la clave en la cabecera X-API-Key.</summary>
public sealed class CaptabotApiClient : ICaptabotApi
{
    public const string CabeceraClave = "X-API-Key";
    public const int TamanoPagina = 20;

    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);
    private static readonly IReadOnlyDictionary<string, string> SinDetalles = new Dictionary<string, string>();

    private readonly HttpClient _http;

    public CaptabotApiClient(HttpClient http, IOptions<CaptabotApiOptions> opciones)
    {
        _http = http;
        var config = opciones.Value;
        _http.BaseAddress ??= new Uri(config.UrlBase.TrimEnd('/') + "/");
        _http.Timeout = TimeSpan.FromSeconds(15);
        if (!string.IsNullOrWhiteSpace(config.ClaveApi))
        {
            _http.DefaultRequestHeaders.Remove(CabeceraClave);
            _http.DefaultRequestHeaders.Add(CabeceraClave, config.ClaveApi);
        }
    }

    public async Task<Usuario?> IniciarSesionAsync(string correo, string contrasena, CancellationToken ct = default)
    {
        using var respuesta = await _http.PostAsJsonAsync("api/auth/login", new { correo, contrasena }, Json, ct);
        if (respuesta.StatusCode == HttpStatusCode.Unauthorized)
        {
            var error = await LeerErrorAsync(respuesta, ct);
            // Un 401 por la clave de la API es un problema de configuración, no del usuario.
            if (error.Mensaje.Contains("clave de la API", StringComparison.OrdinalIgnoreCase))
            {
                throw error;
            }
            return null;
        }
        return await LeerAsync<Usuario>(respuesta, ct);
    }

    public async Task<PaginaResultado<Prospecto>> ListarProspectosAsync(FiltroProspectos filtro,
        CancellationToken ct = default)
    {
        var ruta = ConConsulta("api/prospectos",
            ("calificacion", filtro.Calificacion),
            ("etapa", filtro.Etapa),
            ("desde", Fecha(filtro.Desde)),
            ("hasta", Fecha(filtro.Hasta)),
            ("pagina", Math.Max(filtro.Pagina - 1, 0).ToString(CultureInfo.InvariantCulture)),
            ("tamano", TamanoPagina.ToString(CultureInfo.InvariantCulture)));
        return await GetAsync<PaginaResultado<Prospecto>>(ruta, ct);
    }

    public async Task<Prospecto?> ObtenerProspectoAsync(long id, CancellationToken ct = default)
    {
        using var respuesta = await _http.GetAsync($"api/prospectos/{id}", ct);
        if (respuesta.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }
        return await LeerAsync<Prospecto>(respuesta, ct);
    }

    public async Task<Prospecto> CambiarEtapaAsync(long id, string etapa, long usuarioId, CancellationToken ct = default)
    {
        using var respuesta = await _http.PatchAsJsonAsync($"api/prospectos/{id}/etapa", new { etapa, usuarioId }, Json, ct);
        return await LeerAsync<Prospecto>(respuesta, ct);
    }

    public Task<IReadOnlyList<CambioEtapa>> HistorialAsync(long id, CancellationToken ct = default) =>
        GetAsync<IReadOnlyList<CambioEtapa>>($"api/prospectos/{id}/historial", ct);

    public Task<IReadOnlyList<Nota>> NotasAsync(long id, CancellationToken ct = default) =>
        GetAsync<IReadOnlyList<Nota>>($"api/prospectos/{id}/notas", ct);

    public async Task<Nota> AgregarNotaAsync(long id, long usuarioId, string contenido, CancellationToken ct = default)
    {
        using var respuesta = await _http.PostAsJsonAsync($"api/prospectos/{id}/notas", new { usuarioId, contenido }, Json, ct);
        return await LeerAsync<Nota>(respuesta, ct);
    }

    public async Task<Nota> EditarNotaAsync(long id, long notaId, long usuarioId, string contenido,
        CancellationToken ct = default)
    {
        using var respuesta = await _http.PutAsJsonAsync($"api/prospectos/{id}/notas/{notaId}",
            new { usuarioId, contenido }, Json, ct);
        return await LeerAsync<Nota>(respuesta, ct);
    }

    public Task<IReadOnlyList<Usuario>> ListarUsuariosAsync(CancellationToken ct = default) =>
        GetAsync<IReadOnlyList<Usuario>>("api/usuarios", ct);

    public async Task<Usuario?> ObtenerUsuarioAsync(long id, CancellationToken ct = default)
    {
        using var respuesta = await _http.GetAsync($"api/usuarios/{id}", ct);
        if (respuesta.StatusCode == HttpStatusCode.NotFound)
        {
            return null;
        }
        return await LeerAsync<Usuario>(respuesta, ct);
    }

    public async Task<Usuario> CrearUsuarioAsync(UsuarioSolicitud solicitud, CancellationToken ct = default)
    {
        using var respuesta = await _http.PostAsJsonAsync("api/usuarios", solicitud, Json, ct);
        return await LeerAsync<Usuario>(respuesta, ct);
    }

    public async Task<Usuario> ActualizarUsuarioAsync(long id, UsuarioSolicitud solicitud, CancellationToken ct = default)
    {
        using var respuesta = await _http.PutAsJsonAsync($"api/usuarios/{id}", solicitud, Json, ct);
        return await LeerAsync<Usuario>(respuesta, ct);
    }

    public async Task<Usuario> CambiarEstadoUsuarioAsync(long id, bool activo, CancellationToken ct = default)
    {
        using var respuesta = await _http.PatchAsJsonAsync($"api/usuarios/{id}/estado", new { activo }, Json, ct);
        return await LeerAsync<Usuario>(respuesta, ct);
    }

    public Task<IReadOnlyList<ReporteSemanal>> ReporteSemanalAsync(int semanas, CancellationToken ct = default) =>
        GetAsync<IReadOnlyList<ReporteSemanal>>(
            ConConsulta("api/reportes/semanal", ("semanas", semanas.ToString(CultureInfo.InvariantCulture))), ct);

    public Task<IReadOnlyList<EtapaEmbudo>> EmbudoAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default) =>
        GetAsync<IReadOnlyList<EtapaEmbudo>>(
            ConConsulta("api/reportes/embudo", ("desde", Fecha(desde)), ("hasta", Fecha(hasta))), ct);

    public Task<TasaConversion> ConversionAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default) =>
        GetAsync<TasaConversion>(
            ConConsulta("api/reportes/conversion", ("desde", Fecha(desde)), ("hasta", Fecha(hasta))), ct);

    internal static string ConConsulta(string ruta, params (string Nombre, string? Valor)[] parametros)
    {
        var partes = parametros
            .Where(p => !string.IsNullOrWhiteSpace(p.Valor))
            .Select(p => $"{p.Nombre}={Uri.EscapeDataString(p.Valor!)}");
        var consulta = string.Join("&", partes);
        return consulta.Length == 0 ? ruta : $"{ruta}?{consulta}";
    }

    internal static string? Fecha(DateOnly? fecha) => fecha?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);

    private async Task<T> GetAsync<T>(string ruta, CancellationToken ct)
    {
        using var respuesta = await _http.GetAsync(ruta, ct);
        return await LeerAsync<T>(respuesta, ct);
    }

    private static async Task<T> LeerAsync<T>(HttpResponseMessage respuesta, CancellationToken ct)
    {
        if (!respuesta.IsSuccessStatusCode)
        {
            throw await LeerErrorAsync(respuesta, ct);
        }
        var resultado = await respuesta.Content.ReadFromJsonAsync<T>(Json, ct);
        return resultado ?? throw new ApiException(respuesta.StatusCode, "La API devolvió una respuesta vacía", SinDetalles);
    }

    private static async Task<ApiException> LeerErrorAsync(HttpResponseMessage respuesta, CancellationToken ct)
    {
        var mensaje = $"La API respondió con el código {(int)respuesta.StatusCode}";
        var detalles = SinDetalles;
        try
        {
            var error = await respuesta.Content.ReadFromJsonAsync<ErrorApi>(Json, ct);
            if (!string.IsNullOrWhiteSpace(error?.Error))
            {
                mensaje = error.Error;
            }
            if (error?.Detalles is { Count: > 0 })
            {
                detalles = error.Detalles;
            }
        }
        catch (JsonException)
        {
            // La respuesta no era JSON; se usa el mensaje genérico.
        }
        return new ApiException(respuesta.StatusCode, mensaje, detalles);
    }

    private sealed record ErrorApi(string? Error, Dictionary<string, string>? Detalles);
}
