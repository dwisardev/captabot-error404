using System.Globalization;

namespace CaptaBot.Panel.Ayudas;

/// <summary>Ayudas solo de presentación: cambian cómo se ve un dato en pantalla, nunca el dato guardado.</summary>
public static class Presentacion
{
    /// <summary>
    /// Pone en mayúscula la primera letra de cada palabra que empieza en minúscula («valeria ríos» → «Valeria Ríos»).
    /// Las palabras que ya traen mayúsculas («CI», «McDonald») se dejan tal cual.
    /// </summary>
    public static string Nombre(string? texto)
    {
        if (string.IsNullOrWhiteSpace(texto)) return "";
        var partes = texto.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
        for (var i = 0; i < partes.Length; i++)
        {
            var palabra = partes[i];
            if (char.IsLower(palabra[0]))
                partes[i] = char.ToUpper(palabra[0], CultureInfo.InvariantCulture) + palabra[1..];
        }
        return string.Join(' ', partes);
    }

    /// <summary>Hasta dos iniciales para el avatar («Valeria Ríos» → «VR»).</summary>
    public static string Iniciales(string? nombre)
    {
        var palabras = Nombre(nombre).Split(' ', StringSplitOptions.RemoveEmptyEntries);
        return palabras.Length switch
        {
            0 => "?",
            1 => palabras[0][..1].ToUpperInvariant(),
            _ => (palabras[0][..1] + palabras[^1][..1]).ToUpperInvariant()
        };
    }

    /// <summary>Uno de seis colores de avatar, fijo para cada persona.</summary>
    public static string ClaseAvatar(long id) => "av-" + (int)(Math.Abs(id) % 6);
}
