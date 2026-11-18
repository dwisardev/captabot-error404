using System.Net;

namespace CaptaBot.Panel.Servicios;

/// <summary>La API respondió con un error. El mensaje viene de la API, en español.</summary>
public sealed class ApiException(HttpStatusCode estado, string mensaje, IReadOnlyDictionary<string, string> detalles)
    : Exception(mensaje)
{
    public HttpStatusCode Estado { get; } = estado;

    public IReadOnlyDictionary<string, string> Detalles { get; } = detalles;

    /// <summary>Errores que el usuario puede corregir: datos inválidos, sin permiso, no existe o repetido.</summary>
    public bool EsErrorDelUsuario => Estado is HttpStatusCode.BadRequest or HttpStatusCode.Forbidden
        or HttpStatusCode.NotFound or HttpStatusCode.Conflict;
}
