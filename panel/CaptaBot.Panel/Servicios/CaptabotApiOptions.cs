namespace CaptaBot.Panel.Servicios;

/// <summary>Dirección y clave de la API de CaptaBot. Sección "CaptabotApi" de appsettings.json.</summary>
public sealed class CaptabotApiOptions
{
    public const string Seccion = "CaptabotApi";

    public string UrlBase { get; set; } = "http://localhost:8080";

    /// <summary>Mismo valor que CAPTABOT_API_KEY en la API. Vacío si la API no pide clave.</summary>
    public string? ClaveApi { get; set; }
}
