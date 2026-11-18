using System.Globalization;

namespace CaptaBot.Panel.Ayudas;

/// <summary>Textos y estilos para mostrar los valores de la API en pantalla.</summary>
public static class Etiquetas
{
    /// <summary>Perú no cambia de horario: UTC-5 todo el año.</summary>
    public static readonly TimeSpan HoraPeru = TimeSpan.FromHours(-5);

    public static readonly IReadOnlyList<(string Valor, string Texto)> Etapas =
    [
        ("NUEVO", "Nuevo"),
        ("CONTACTADO", "Contactado"),
        ("COTIZADO", "Cotizado"),
        ("CERRADO_GANADO", "Cerrado – ganado"),
        ("CERRADO_PERDIDO", "Cerrado – perdido")
    ];

    public static readonly IReadOnlyList<(string Valor, string Texto)> Calificaciones =
    [
        ("CALIENTE", "Caliente"),
        ("TIBIO", "Tibio"),
        ("FRIO", "Frío")
    ];

    public static readonly IReadOnlyList<(string Valor, string Texto)> Roles =
    [
        ("VENDEDOR", "Vendedor"),
        ("ADMINISTRADOR", "Administrador")
    ];

    public static string TextoEtapa(string? valor) => Buscar(Etapas, valor);

    public static string TextoCalificacion(string? valor) => Buscar(Calificaciones, valor);

    public static string TextoRol(string? valor) => Buscar(Roles, valor);

    public static bool EsEtapa(string? valor) => Etapas.Any(e => e.Valor == valor);

    public static bool EsCalificacion(string? valor) => Calificaciones.Any(c => c.Valor == valor);

    public static bool EsRol(string? valor) => Roles.Any(r => r.Valor == valor);

    public static string ClaseCalificacion(string? valor) => valor switch
    {
        "CALIENTE" => "chip chip-caliente",
        "TIBIO" => "chip chip-tibio",
        "FRIO" => "chip chip-frio",
        _ => "chip"
    };

    public static string ClaseEtapa(string? valor) => valor switch
    {
        "CERRADO_GANADO" => "chip chip-ganado",
        "CERRADO_PERDIDO" => "chip chip-perdido",
        _ => "chip chip-etapa"
    };

    public static string FechaHora(DateTimeOffset fecha) =>
        fecha.ToOffset(HoraPeru).ToString("dd/MM/yyyy HH:mm", CultureInfo.InvariantCulture);

    public static string Fecha(DateTimeOffset fecha) =>
        fecha.ToOffset(HoraPeru).ToString("dd/MM/yyyy", CultureInfo.InvariantCulture);

    public static string Fecha(DateOnly fecha) => fecha.ToString("dd/MM/yyyy", CultureInfo.InvariantCulture);

    public static string Soles(decimal monto) => "S/ " + monto.ToString("N2", CultureInfo.InvariantCulture);

    /// <summary>Enlace de WhatsApp para un celular peruano de 9 dígitos.</summary>
    public static string WhatsApp(string telefono) => "https://wa.me/51" + telefono;

    private static string Buscar(IReadOnlyList<(string Valor, string Texto)> lista, string? valor) =>
        lista.FirstOrDefault(e => e.Valor == valor).Texto ?? valor ?? "";
}
